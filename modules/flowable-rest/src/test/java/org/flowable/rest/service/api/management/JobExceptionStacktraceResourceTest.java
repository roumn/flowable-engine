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
package org.flowable.rest.service.api.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.job.api.Job;
import org.flowable.rest.service.BaseSpringRestTestCase;
import org.flowable.rest.service.api.RestUrls;
import org.junit.jupiter.api.Test;

/**
 * Test for all REST-operations related to the Job collection and a single job resource.
 * 
 * @author Frederik Heremans
 */
public class JobExceptionStacktraceResourceTest extends BaseSpringRestTestCase {

    /**
     * Test getting the stacktrace for a failed job
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/management/JobExceptionStacktraceResourceTest.testTimerProcess.bpmn20.xml" })
    public void testGetJobStacktrace() throws Exception {
        // Start process, forcing error on job-execution
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerProcess", Collections.singletonMap("error", (Object) Boolean.TRUE));

        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();

        // Force execution of job
        Job timerJob2 = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).timers().singleResult();
        assertThatThrownBy(() -> {
            managementService.moveTimerToExecutableJob(timerJob2.getId());
            managementService.executeJob(timerJob2.getId());
        })
                .isExactlyInstanceOf(FlowableException.class);

        Calendar now = Calendar.getInstance();
        now.set(Calendar.MILLISECOND, 0);
        processEngineConfiguration.getClock().setCurrentTime(now.getTime());

        CloseableHttpResponse response = executeRequest(new HttpGet(
                SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_EXCEPTION_STRACKTRACE, timerJob.getId())), HttpStatus.SC_OK);

        String stack = IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8);
        assertThat(stack).isEqualTo(managementService.getTimerJobExceptionStacktrace(timerJob.getId()));

        // Also check content-type
        assertThat(response.getEntity().getContentType().getValue()).isEqualTo("text/plain");
        closeResponse(response);
    }

    /**
     * Test getting the stacktrace for an unexisting job.
     */
    @Test
    public void testGetStackForUnexistingJob() throws Exception {
        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_JOB_EXCEPTION_STRACKTRACE, "unexistingjob")), HttpStatus.SC_NOT_FOUND));
    }

    /**
     * Test getting the stacktrace for an unexisting job.
     */
    @Test
    @Deployment(resources = { "org/flowable/rest/service/api/management/JobExceptionStacktraceResourceTest.testTimerProcess.bpmn20.xml" })
    public void testGetStackForJobWithoutException() throws Exception {
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("timerProcess", Collections.singletonMap("error", (Object) Boolean.FALSE));
        Job timerJob = managementService.createTimerJobQuery().processInstanceId(processInstance.getId()).singleResult();
        assertThat(timerJob).isNotNull();

        closeResponse(executeRequest(new HttpGet(SERVER_URL_PREFIX + RestUrls.createRelativeResourceUrl(RestUrls.URL_TIMER_JOB_EXCEPTION_STRACKTRACE, timerJob.getId())), HttpStatus.SC_NOT_FOUND));
    }
}
