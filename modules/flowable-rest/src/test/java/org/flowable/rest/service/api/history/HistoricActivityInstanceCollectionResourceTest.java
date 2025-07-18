/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.rest.service.api.history;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.engine.impl.cmd.ChangeDeploymentTenantIdCmd;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Test for REST-operation related to the historic activity instance query resource.
 * 
 * @author Tijs Rademakers
 */
public class HistoricActivityInstanceCollectionResourceTest extends BaseSpringRestTestCase {

    /**
     * Test querying historic activity instance. GET history/historic-activity-instances
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/twoTaskProcess.bpmn20.xml" })
    public void testQueryActivityInstances() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess");
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        taskService.complete(task.getId());

        // Set tenant on deployment
        managementService.executeCommand(new ChangeDeploymentTenantIdCmd(deploymentId, "myTenant"));

        ProcessInstance processInstance2 = runtimeService.startProcessInstanceByKeyAndTenantId("oneTaskProcess", "myTenant");

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_HISTORIC_ACTIVITY_INSTANCES);

        assertResultsPresentInDataResponse(url + "?activityId=processTask", 2, "processTask");

        assertResultsPresentInDataResponse(url + "?activityId=processTask&finished=true", 1, "processTask");

        assertResultsPresentInDataResponse(url + "?activityId=processTask&finished=false", 1, "processTask");

        assertResultsPresentInDataResponse(url + "?activityId=processTask2", 1, "processTask2");

        assertResultsPresentInDataResponse(url + "?activityId=processTask3", 0);

        assertResultsPresentInDataResponse(url + "?activityName=Process%20task", 2, "processTask");

        assertResultsPresentInDataResponse(url + "?activityName=Process%20task2", 1, "processTask2");

        assertResultsPresentInDataResponse(url + "?activityName=Process%20task3", 0);

        assertResultsPresentInDataResponse(url + "?activityType=userTask", 3, "processTask", "processTask2");

        assertResultsPresentInDataResponse(url + "?activityType=startEvent", 2, "theStart");

        assertResultsPresentInDataResponse(url + "?activityType=receiveTask", 0);

        assertResultsPresentInDataResponse(url + "?processInstanceId=" + processInstance.getId(), 5, "theStart", "flow1", "processTask", "flow2", "processTask2");

        assertResultsPresentInDataResponse(url + "?processInstanceId=" + processInstance2.getId(), 3, "theStart", "flow1", "processTask");

        assertResultsPresentInDataResponse(url + "?processDefinitionId=" + processInstance.getProcessDefinitionId(), 8,
            "theStart", "flow1",  "processTask", "flow2", "processTask2");

        assertResultsPresentInDataResponse(url + "?taskAssignee=kermit", 2, "processTask");

        assertResultsPresentInDataResponse(url + "?taskAssignee=fozzie", 1, "processTask2");

        assertResultsPresentInDataResponse(url + "?taskAssignee=fozzie2", 0);

        // Without tenant ID, only activities for processinstance1
        assertResultsPresentInDataResponse(url + "?withoutTenantId=true", 5);

        // Tenant id
        assertResultsPresentInDataResponse(url + "?tenantId=myTenant", 3, "theStart", "flow1", "processTask");
        assertResultsPresentInDataResponse(url + "?tenantId=anotherTenant");

        // Tenant id like
        assertResultsPresentInDataResponse(url + "?tenantIdLike=" + encode("%enant"), 3, "theStart", "flow1", "processTask");
        assertResultsPresentInDataResponse(url + "?tenantIdLike=anotherTenant");
    }

    protected void assertResultsPresentInDataResponse(String url, int numberOfResultsExpected, String... expectedActivityIds) throws JsonProcessingException, IOException {
        // Do the actual call
        CloseableHttpResponse response = executeRequest(new HttpGet(SERVER_URL_PREFIX + url), HttpStatus.SC_OK);
        JsonNode dataNode = objectMapper.readTree(response.getEntity().getContent()).get("data");
        closeResponse(response);
        assertThat(dataNode).hasSize(numberOfResultsExpected);

        // Check presence of ID's
        if (expectedActivityIds != null) {
            List<String> toBeFound = new ArrayList<>(Arrays.asList(expectedActivityIds));
            Iterator<JsonNode> it = dataNode.iterator();
            while (it.hasNext()) {
                String activityId = it.next().get("activityId").textValue();
                toBeFound.remove(activityId);
            }
            assertThat(toBeFound).as("Not all entries have been found in result, missing: " + StringUtils.join(toBeFound, ", ")).isEmpty();
        }
    }
}
