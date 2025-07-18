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

package org.flowable.rest.service.api.runtime;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.javacrumbs.jsonunit.core.Option;

/**
 * Test for all REST-operations related to the Task collection resource.
 * 
 * @author Frederik Heremans
 */
public class TaskQueryResourceTest extends BaseSpringRestTestCase {

    /**
     * Test querying tasks. GET runtime/tasks
     */
    @Test
    @Deployment
    public void testQueryTasks() throws Exception {
        try {
            Calendar adhocTaskCreate = Calendar.getInstance();
            adhocTaskCreate.set(Calendar.MILLISECOND, 0);

            Calendar processTaskCreate = Calendar.getInstance();
            processTaskCreate.add(Calendar.HOUR, 2);
            processTaskCreate.set(Calendar.MILLISECOND, 0);

            Calendar inBetweenTaskCreation = Calendar.getInstance();
            inBetweenTaskCreation.add(Calendar.HOUR, 1);

            processEngineConfiguration.getClock().setCurrentTime(adhocTaskCreate.getTime());
            Task adhocTask = taskService.newTask();
            adhocTask.setAssignee("gonzo");
            adhocTask.setOwner("owner");
            adhocTask.setDelegationState(DelegationState.PENDING);
            adhocTask.setDescription("Description one");
            adhocTask.setName("Name one");
            adhocTask.setDueDate(adhocTaskCreate.getTime());
            adhocTask.setPriority(100);
            adhocTask.setFormKey("myForm.json");
            adhocTask.setCategory("some-category");
            taskService.saveTask(adhocTask);
            taskService.addUserIdentityLink(adhocTask.getId(), "misspiggy", IdentityLinkType.PARTICIPANT);

            processEngineConfiguration.getClock().setCurrentTime(processTaskCreate.getTime());
            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", "myBusinessKey");
            Task processTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            processTask.setParentTaskId(adhocTask.getId());
            processTask.setPriority(50);
            processTask.setDueDate(processTaskCreate.getTime());
            taskService.saveTask(processTask);

            // Check filter-less to fetch all tasks
            String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_QUERY);
            ObjectNode requestNode = objectMapper.createObjectNode();
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId(), adhocTask.getId());

            // ID filtering
            requestNode.removeAll();
            requestNode.put("taskId", adhocTask.getId());
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Name filtering
            requestNode.removeAll();
            requestNode.put("name", "Name one");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Name like filtering
            requestNode.removeAll();
            requestNode.put("nameLike", "%one");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Description filtering
            requestNode.removeAll();
            requestNode.put("description", "Description one");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Description like filtering
            requestNode.removeAll();
            requestNode.put("descriptionLike", "%one");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Priority filtering
            requestNode.removeAll();
            requestNode.put("priority", 100);
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Minimum Priority filtering
            requestNode.removeAll();
            requestNode.put("minimumPriority", 70);
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Maximum Priority filtering
            requestNode.removeAll();
            requestNode.put("maximumPriority", 70);
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

            // Owner filtering
            requestNode.removeAll();
            requestNode.put("owner", "owner");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Assignee filtering
            requestNode.removeAll();
            requestNode.put("assignee", "gonzo");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Owner like filtering
            requestNode.removeAll();
            requestNode.put("ownerLike", "owne%");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Assignee like filtering
            requestNode.removeAll();
            requestNode.put("assigneeLike", "%onzo");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Unassigned filtering
            requestNode.removeAll();
            requestNode.put("unassigned", true);
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

            // Delegation state filtering
            requestNode.removeAll();
            requestNode.put("delegationState", "pending");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Candidate user filtering
            requestNode.removeAll();
            requestNode.put("candidateUser", "kermit");
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

            // Candidate group filtering
            requestNode.removeAll();
            requestNode.put("candidateGroup", "sales");
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

            // Candidate group In filtering
            requestNode.removeAll();
            ArrayNode arrayNode = requestNode.arrayNode();

            arrayNode.add("sales");
            arrayNode.add("someOtherGroup");

            requestNode.set("candidateGroupIn", arrayNode);
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

            // Involved user filtering
            requestNode.removeAll();
            requestNode.put("involvedUser", "misspiggy");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Claim task
            taskService.claim(processTask.getId(), "johnDoe");

            // IgnoreAssignee
            requestNode.removeAll();
            requestNode.put("candidateGroup", "sales");
            requestNode.put("ignoreAssignee", true);
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

            // Process instance filtering
            requestNode.removeAll();
            requestNode.put("processInstanceId", processInstance.getId());
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());
            
            // Process instance with children filtering
            requestNode.removeAll();
            requestNode.put("processInstanceIdWithChildren", processInstance.getId());
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());
            
            requestNode.removeAll();
            requestNode.put("processInstanceIdWithChildren", "nonexisting");
            assertResultsPresentInPostDataResponse(url, requestNode);
            
            // Without process instance id filtering
            requestNode.removeAll();
            requestNode.put("withoutProcessInstanceId", true);
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());
            
            requestNode.removeAll();
            requestNode.put("withoutProcessInstanceId", false);
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId(), adhocTask.getId());

            // Execution filtering
            requestNode.removeAll();
            Execution taskExecution = runtimeService.createExecutionQuery().activityId("processTask").singleResult();
            requestNode.put("executionId", taskExecution.getId());
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

            // Process instance businesskey filtering
            requestNode.removeAll();
            requestNode.put("processInstanceBusinessKey", "myBusinessKey");
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

            // Process instance businesskey like filtering
            requestNode.removeAll();
            requestNode.put("processInstanceBusinessKeyLike", "myBusiness%");
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

            // Process definition key
            requestNode.removeAll();
            requestNode.put("processDefinitionKey", "oneTaskProcess");
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

            // Process definition key like
            requestNode.removeAll();
            requestNode.put("processDefinitionKeyLike", "%TaskProcess");
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

            // Process definition name
            requestNode.removeAll();
            requestNode.put("processDefinitionName", "The One Task Process");
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

            // Process definition name like
            requestNode.removeAll();
            requestNode.put("processDefinitionNameLike", "The One %");
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

            // CreatedOn filtering
            requestNode.removeAll();
            requestNode.put("createdOn", getISODateString(adhocTaskCreate.getTime()));
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // CreatedAfter filtering
            requestNode.removeAll();
            requestNode.put("createdAfter", getISODateString(inBetweenTaskCreation.getTime()));
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

            // CreatedBefore filtering
            requestNode.removeAll();
            requestNode.put("createdBefore", getISODateString(inBetweenTaskCreation.getTime()));
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Subtask exclusion
            requestNode.removeAll();
            requestNode.put("excludeSubTasks", true);
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Task definition key filtering
            requestNode.removeAll();
            requestNode.put("taskDefinitionKey", "processTask");
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

            // Task definition key like filtering
            requestNode.removeAll();
            requestNode.put("taskDefinitionKeyLike", "process%");
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

            // Task definition keys filtering
            requestNode.removeAll();
            requestNode.putArray("taskDefinitionKeys").add("processTask").add("invalidTask");
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

            // Duedate filtering
            requestNode.removeAll();
            requestNode.put("dueDate", getISODateString(adhocTaskCreate.getTime()));
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Due after filtering
            requestNode.removeAll();
            requestNode.put("dueAfter", getISODateString(inBetweenTaskCreation.getTime()));
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

            // Due before filtering
            requestNode.removeAll();
            requestNode.put("dueBefore", getISODateString(inBetweenTaskCreation.getTime()));
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Suspend process-instance to have a suspended task
            runtimeService.suspendProcessInstanceById(processInstance.getId());

            // Suspended filtering
            requestNode.removeAll();
            requestNode.put("active", false);
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

            // Active filtering
            requestNode.removeAll();
            requestNode.put("active", true);
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());

            // Filtering by category
            requestNode.removeAll();
            requestNode.put("category", "some-category");
            assertResultsPresentInPostDataResponse(url, requestNode, adhocTask.getId());
            
            // Without scope id filtering
            requestNode.removeAll();
            requestNode.put("withoutScopeId", true);
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId(), adhocTask.getId());

            // Filtering without duedate
            requestNode.removeAll();
            requestNode.put("withoutDueDate", true);
            // No response should be returned, no tasks without a duedate yet
            assertResultsPresentInPostDataResponse(url, requestNode);

            processTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
            processTask.setDueDate(null);
            taskService.saveTask(processTask);
            assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                if (task.getExecutionId() == null) {
                    taskService.deleteTask(task.getId(), true);
                }
            }
        }
    }

    /**
     * Test querying tasks using task and process variables. GET runtime/tasks
     */
    @Test
    @Deployment
    public void testQueryTasksWithVariables() throws Exception {
        HashMap<String, Object> processVariables = new HashMap<>();
        processVariables.put("stringVar", "Azerty");
        processVariables.put("intVar", 67890);
        processVariables.put("booleanVar", false);

        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("oneTaskProcess", processVariables);
        Task processTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();

        HashMap<String, Object> variables = new HashMap<>();
        variables.put("stringVar", "Abcdef");
        variables.put("intVar", 12345);
        variables.put("booleanVar", true);
        taskService.setVariablesLocal(processTask.getId(), variables);

        // Additional tasks to confirm it's filtered out
        runtimeService.startProcessInstanceByKey("oneTaskProcess");

        ObjectNode requestNode = objectMapper.createObjectNode();
        ArrayNode variableArray = objectMapper.createArrayNode();
        ObjectNode variableNode = objectMapper.createObjectNode();
        variableArray.add(variableNode);
        requestNode.set("taskVariables", variableArray);

        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_QUERY);

        // String equals
        variableNode.put("name", "stringVar");
        variableNode.put("value", "Abcdef");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // Integer equals
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 12345);
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // Boolean equals
        variableNode.removeAll();
        variableNode.put("name", "booleanVar");
        variableNode.put("value", true);
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // String not equals
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "ghijkl");
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // Integer not equals
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 45678);
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // Boolean not equals
        variableNode.removeAll();
        variableNode.put("name", "booleanVar");
        variableNode.put("value", false);
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // String equals ignore case
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "abCDEF");
        variableNode.put("operation", "equalsIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // String not equals ignore case
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "HIJKLm");
        variableNode.put("operation", "notEqualsIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // String equals without value
        variableNode.removeAll();
        variableNode.put("value", "Abcdef");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // Greater than
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 12300);
        variableNode.put("operation", "greaterThan");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());
        variableNode.put("value", 12345);
        variableNode.put("operation", "greaterThan");
        assertResultsPresentInPostDataResponse(url, requestNode);

        // Greater than or equal
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 12300);
        variableNode.put("operation", "greaterThanOrEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());
        variableNode.put("value", 12345);
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // Less than
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 12400);
        variableNode.put("operation", "lessThan");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());
        variableNode.put("value", 12345);
        variableNode.put("operation", "lessThan");
        assertResultsPresentInPostDataResponse(url, requestNode);

        // Less than or equal
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 12400);
        variableNode.put("operation", "lessThanOrEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());
        variableNode.put("value", 12345);
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // Like
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "Abcde%");
        variableNode.put("operation", "like");

        // Any other operation but equals without value
        variableNode.removeAll();
        variableNode.put("value", "abcdef");
        variableNode.put("operation", "notEquals");

        assertResultsPresentInPostDataResponseWithStatusCheck(url, requestNode, HttpStatus.SC_BAD_REQUEST);

        // Illegal (but existing) operation
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "abcdef");
        variableNode.put("operation", "operationX");

        assertResultsPresentInPostDataResponseWithStatusCheck(url, requestNode, HttpStatus.SC_BAD_REQUEST);

        // Process variables
        requestNode = objectMapper.createObjectNode();
        variableArray = objectMapper.createArrayNode();
        variableNode = objectMapper.createObjectNode();
        variableArray.add(variableNode);
        requestNode.set("processInstanceVariables", variableArray);

        // String equals
        variableNode.put("name", "stringVar");
        variableNode.put("value", "Azerty");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // Integer equals
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 67890);
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // Boolean equals
        variableNode.removeAll();
        variableNode.put("name", "booleanVar");
        variableNode.put("value", false);
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // String not equals
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "ghijkl");
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // Integer not equals
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 45678);
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // Boolean not equals
        variableNode.removeAll();
        variableNode.put("name", "booleanVar");
        variableNode.put("value", true);
        variableNode.put("operation", "notEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // String equals ignore case
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "azeRTY");
        variableNode.put("operation", "equalsIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // String not equals ignore case
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "HIJKLm");
        variableNode.put("operation", "notEqualsIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // String equals without value
        variableNode.removeAll();
        variableNode.put("value", "Azerty");
        variableNode.put("operation", "equals");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // Greater than
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 67800);
        variableNode.put("operation", "greaterThan");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());
        variableNode.put("value", 67890);
        variableNode.put("operation", "greaterThan");
        assertResultsPresentInPostDataResponse(url, requestNode);

        // Greater than or equal
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 67800);
        variableNode.put("operation", "greaterThanOrEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());
        variableNode.put("value", 67890);
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // Less than
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 67900);
        variableNode.put("operation", "lessThan");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());
        variableNode.put("value", 67890);
        variableNode.put("operation", "lessThan");
        assertResultsPresentInPostDataResponse(url, requestNode);

        // Less than or equal
        variableNode.removeAll();
        variableNode.put("name", "intVar");
        variableNode.put("value", 67900);
        variableNode.put("operation", "lessThanOrEquals");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());
        variableNode.put("value", 67890);
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // Like
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "Azert%");
        variableNode.put("operation", "like");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());

        // LikeIgnore Case
        variableNode.removeAll();
        variableNode.put("name", "stringVar");
        variableNode.put("value", "AzErT%");
        variableNode.put("operation", "likeIgnoreCase");
        assertResultsPresentInPostDataResponse(url, requestNode, processTask.getId());
    }

    @Test
    public void testQueryTaskWithCategory() throws Exception {
        Task t1 = taskService.createTaskBuilder().name("t1").category("Cat 1").create();
        Task t2 = taskService.createTaskBuilder().name("t2").create();

        Task t3 = taskService.createTaskBuilder().name("t3").category("Cat 2").create();
        taskService.saveTask(t1);
        taskService.saveTask(t2);
        taskService.saveTask(t3);
        try {
            String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_QUERY);

            ObjectNode requestNode = objectMapper.createObjectNode();
            requestNode.put("withoutCategory", true);
            assertResultsPresentInPostDataResponse(url, requestNode, t2.getId());

            requestNode = objectMapper.createObjectNode();
            requestNode.putArray("categoryIn").add("Cat 1").add("Cat 2");
            assertResultsPresentInPostDataResponse(url, requestNode, t1.getId(), t3.getId());

            requestNode = objectMapper.createObjectNode();
            requestNode.putArray("categoryNotIn").add("Cat 1");
            assertResultsPresentInPostDataResponse(url, requestNode, t3.getId());

        } finally {
            deleteTasks(t1, t2, t3);
        }
    }

    private void deleteTasks(Task... tasks) {
        if (tasks != null) {
            Arrays.asList(tasks).forEach(t -> {
                taskService.deleteTask(t.getId());
                historyService.deleteHistoricTaskInstance(t.getId());
            });
        }
    }

    /**
     * Test querying tasks. GET runtime/tasks
     */
    @Test
    public void testQueryTasksWithPaging() throws Exception {
        try {
            Calendar adhocTaskCreate = Calendar.getInstance();
            adhocTaskCreate.set(Calendar.MILLISECOND, 0);

            processEngineConfiguration.getClock().setCurrentTime(adhocTaskCreate.getTime());
            List<String> taskIdList = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                Task adhocTask = taskService.newTask();
                adhocTask.setAssignee("gonzo");
                adhocTask.setOwner("owner");
                adhocTask.setDelegationState(DelegationState.PENDING);
                adhocTask.setDescription("Description one");
                adhocTask.setName("Name one");
                adhocTask.setDueDate(adhocTaskCreate.getTime());
                adhocTask.setPriority(100);
                taskService.saveTask(adhocTask);
                taskService.addUserIdentityLink(adhocTask.getId(), "misspiggy", IdentityLinkType.PARTICIPANT);
                taskIdList.add(adhocTask.getId());
            }
            Collections.sort(taskIdList);

            // Check filter-less to fetch all tasks
            String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_QUERY);
            ObjectNode requestNode = objectMapper.createObjectNode();
            String[] taskIds = new String[] { taskIdList.get(0), taskIdList.get(1), taskIdList.get(2) };
            assertResultsPresentInPostDataResponse(url + "?size=3&sort=id&order=asc", requestNode, taskIds);

            taskIds = new String[] { taskIdList.get(4), taskIdList.get(5), taskIdList.get(6), taskIdList.get(7) };
            assertResultsPresentInPostDataResponse(url + "?start=4&size=4&sort=id&order=asc", requestNode, taskIds);

            taskIds = new String[] { taskIdList.get(8), taskIdList.get(9) };
            assertResultsPresentInPostDataResponse(url + "?start=8&size=10&sort=id&order=asc", requestNode, taskIds);

        } finally {
            // Clean adhoc-tasks even if test fails
            List<Task> tasks = taskService.createTaskQuery().list();
            for (Task task : tasks) {
                if (task.getExecutionId() == null) {
                    taskService.deleteTask(task.getId(), true);
                }
            }
        }
    }

    @Test
    @Deployment(resources = "org/flowable/rest/service/api/runtime/TaskQueryResourceTest.testQueryTasks.bpmn20.xml", tenantId = "testTenant")
    public void testQueryTasksWithTenant() throws Exception {
        runtimeService.startProcessInstanceByKeyAndTenantId("oneTaskProcess", "myBusinessKey",
            Collections.singletonMap("var1", "var1Value"),
            "testTenant");

        // Check filter-less to fetch all tasks
        String url = RestUrls.createRelativeResourceUrl(RestUrls.URL_TASK_QUERY);
        ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.put("includeProcessVariables", true);
        requestNode.put("includeTaskLocalVariables", true);
        assertTenantIdPresent(url, requestNode, "testTenant");
    }

    protected void assertTenantIdPresent(String url, ObjectNode requestNode, String tenantId) throws IOException {
        // Do the actual call
        HttpPost post = new HttpPost(SERVER_URL_PREFIX + url);
        post.setEntity(new StringEntity(requestNode.toString()));
        CloseableHttpResponse response = executeRequest(post, HttpStatus.SC_OK);

        // Check status and size
        JsonNode rootNode = objectMapper.readTree(response.getEntity().getContent());
        assertThatJson(rootNode)
                .when(Option.IGNORING_EXTRA_FIELDS)
                .isEqualTo("{"
                        + "data: [ {"
                        + "          tenantId: 'testTenant'"
                        + "      } ]"
                        + "}");
        closeResponse(response);
    }

}
