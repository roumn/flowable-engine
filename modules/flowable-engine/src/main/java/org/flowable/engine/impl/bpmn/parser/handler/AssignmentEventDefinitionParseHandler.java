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
package org.flowable.engine.impl.bpmn.parser.handler;

import java.util.Arrays;

import org.flowable.bpmn.model.AssignmentEventDefinition;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.bpmn.model.ErrorEventDefinition;
import org.flowable.bpmn.model.FieldExtension;
import org.flowable.bpmn.model.FlowableListener;
import org.flowable.bpmn.model.ImplementationType;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.delegate.TransactionDependentExecutionListener;
import org.flowable.engine.impl.bpmn.helper.AssignmentTaskListener;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;
import org.flowable.task.service.delegate.BaseTaskListener;

/**
 * @author Roman Saratz
 */
public class AssignmentEventDefinitionParseHandler extends AbstractBpmnParseHandler<AssignmentEventDefinition> {

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return AssignmentEventDefinition.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, AssignmentEventDefinition eventDefinition) {
        if (bpmnParse.getCurrentFlowElement() instanceof BoundaryEvent) {
            BoundaryEvent boundaryEvent = (BoundaryEvent) bpmnParse.getCurrentFlowElement();
            boundaryEvent.setBehavior(
                    bpmnParse.getActivityBehaviorFactory().createBoundaryAssignmentEventActivityBehavior(boundaryEvent, eventDefinition, false));
            FlowableListener listener = new FlowableListener();
            listener.setImplementationType(ImplementationType.IMPLEMENTATION_TYPE_CLASS);
            listener.setImplementation(AssignmentTaskListener.class.getCanonicalName());
            listener.setEvent(BaseTaskListener.EVENTNAME_ASSIGNMENT);
            listener.setOnTransaction(TransactionDependentExecutionListener.ON_TRANSACTION_COMMITTED);
            FieldExtension fieldExtension = new FieldExtension();
            fieldExtension.setFieldName("boundaryEventActivityId");
            fieldExtension.setStringValue(boundaryEvent.getId());
            listener.setFieldExtensions(Arrays.asList(fieldExtension));
            ((UserTask) boundaryEvent.getAttachedToRef()).getTaskListeners().add(listener);
        }
    }
}
