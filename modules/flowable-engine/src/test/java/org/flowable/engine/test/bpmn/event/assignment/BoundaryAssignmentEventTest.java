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
package org.flowable.engine.test.bpmn.event.assignment;

import static org.assertj.core.api.Assertions.assertThat;

import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.test.Deployment;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;

/**
 * @author Roman Saratz
 */
public class BoundaryAssignmentEventTest extends PluggableFlowableTestCase {

    @Test
    @Deployment
    public void testAssignmentBoundaryEvent() {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("assignmentBoundaryEventTestProcess");

        Task task = taskService.createTaskQuery().singleResult();

        assertThat(getAssignmentCount(instance.getId())).isEqualTo(0L);

        taskService.claim(task.getId(), "kermit");

        assertThat(getAssignmentCount(instance.getId())).isEqualTo(1L);

        taskService.setAssignee(task.getId(), "ernie");

        assertThat(getAssignmentCount(instance.getId())).isEqualTo(2L);

    }

    private Object getAssignmentCount(String processInstanceId) {
        return processEngineConfiguration.getCommandExecutor().execute(commandContext -> runtimeService.getVariable(processInstanceId, "assignmentCount"));
    }
}
