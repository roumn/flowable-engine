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
package org.flowable.engine.impl.bpmn.helper;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.flowable.bpmn.model.Task;
import org.flowable.engine.delegate.TransactionDependentTaskListener;
import org.flowable.engine.impl.el.FixedValue;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * @author Roman Saratz
 */
public class AssignmentTaskListener implements TransactionDependentTaskListener {

    private FixedValue boundaryEventActivityId;

    @Override
    public void notify(String processInstanceId, String executionId, Task task, Map<String, Object> executionVariables,
            Map<String, Object> customPropertiesMap) {

        List<ExecutionEntity> executions = CommandContextUtil.getExecutionEntityManager().findChildExecutionsByProcessInstanceId(processInstanceId);

        Optional<ExecutionEntity> boundaryEventExecution = executions.stream()
                .filter(execution -> Objects.equals(execution.getActivityId(), boundaryEventActivityId.getValue(null)))
                .findFirst();

        boundaryEventExecution.ifPresent(execution -> CommandContextUtil.getAgenda().planTriggerExecutionOperation(execution));

    }
}
