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
package org.flowable.cmmn.test.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.api.history.HistoricPlanItemInstance;
import org.flowable.cmmn.api.runtime.CaseInstance;
import org.flowable.cmmn.api.runtime.PlanItemDefinitionType;
import org.flowable.cmmn.api.runtime.PlanItemInstance;
import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.cmmn.engine.test.impl.CmmnHistoryTestHelper;
import org.flowable.cmmn.test.FlowableCmmnTestCase;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.scripting.FlowableScriptEvaluationException;
import org.flowable.variable.api.history.HistoricVariableInstance;
import org.junit.jupiter.api.Test;

/**
 * @author Dennis Federico
 * @author Filip Hrisafov
 */
public class CmmnScriptTaskTest extends FlowableCmmnTestCase {

    @Test
    @CmmnDeployment
    public void testSimpleScript() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .start();
        assertThat(caseInstance).isNotNull();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItemTaskA").singleResult();
        assertThat(planItemInstance)
                .extracting(PlanItemInstance::getName, PlanItemInstance::getPlanItemDefinitionId)
                .containsExactly("Plan Item One", "taskA");
        assertCaseInstanceNotEnded(caseInstance);

        planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("blockerPlanItem").singleResult();
        assertThat(planItemInstance).isNotNull();
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
        assertCaseInstanceEnded(caseInstance);

        Map<String, Object> variables = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertThat(variables).isEmpty();
    }

    @Test
    @CmmnDeployment
    public void testInputVariables() {
        // When input parameters are defined then doNotIncludeVariables is ignored
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .variable("a", 20)
                .variable("b", 22)
                .variable("aVar", 100)
                .variable("bVar", 10)
                .start();
        assertThat(caseInstance).isNotNull();
        assertThat(caseInstance.getCaseVariables().get("sum"))
                .isInstanceOfSatisfying(Number.class, number -> assertThat(number.intValue()).isEqualTo(120));
    }

    @Test
    @CmmnDeployment
    public void testDoNotIncludeVariables() {
        assertThatThrownBy(() -> cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .variable("a", 20)
                .variable("b", 22)
                .start())
                .hasMessageContaining("\"a\" is not defined");
    }

    @Test
    @CmmnDeployment
    public void testPlanItemInstanceVarScopeAndVarHistory() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .start();
        assertThat(caseInstance).isNotNull();

        //Keep the ScriptTaskPlanItem id to check the historic scope later
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItemTaskA").singleResult();
        assertThat(planItemInstance).isNotNull();
        String scriptTaskPlanInstanceId = planItemInstance.getId();

        //No variables set and no variables created yet by the script, because it has not run
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).isEmpty();
        assertThat(cmmnRuntimeService.getLocalVariables(scriptTaskPlanInstanceId)).isEmpty();

        //Initialize one of the script variables to set its scope local to the planItemInstance, otherwise it goes up in the hierarchy up to the case by default
        cmmnRuntimeService.setLocalVariable(scriptTaskPlanInstanceId, "aString", "VALUE TO OVERWRITE");
        assertThat(cmmnRuntimeService.getLocalVariables(scriptTaskPlanInstanceId)).hasSize(1);
        assertThat(cmmnRuntimeService.getVariables(caseInstance.getId())).isEmpty();

        //Trigger the task entry event
        PlanItemInstance blocker = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("taskBlocker").singleResult();
        assertThat(blocker).isNotNull();
        cmmnRuntimeService.triggerPlanItemInstance(blocker.getId());

        //The case has not ended yet
        assertCaseInstanceNotEnded(caseInstance);

        //Check the case variables, one will be created by the script execution
        Map<String, Object> caseVariables = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertThat(caseVariables).hasSize(1);
        assertThat(cmmnRuntimeService.hasVariable(caseInstance.getId(), "aInt")).isTrue();
        Object integer = cmmnRuntimeService.getVariable(caseInstance.getId(), "aInt");
        assertThat(integer)
                .isInstanceOf(Integer.class)
                .isEqualTo(5);

        //The planItemInstance scope variable is available on the history service
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricVariableInstance> historicVariables = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .planItemInstanceId(scriptTaskPlanInstanceId)
                    .list();
            assertThat(historicVariables).hasSize(1);

            HistoricVariableInstance planItemInstanceVariable = historicVariables.get(0);
            assertThat(planItemInstanceVariable).isNotNull();
            assertThat(planItemInstanceVariable.getVariableName()).isEqualTo("aString");
            assertThat(planItemInstanceVariable.getVariableTypeName()).isEqualTo("string");
            assertThat(planItemInstanceVariable.getValue()).isEqualTo("value set in the script");
            assertThat(planItemInstanceVariable.getSubScopeId()).isEqualTo(scriptTaskPlanInstanceId);
        }

        endTestCase();
        assertCaseInstanceEnded(caseInstance);

        //Both variables are still in the history
        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            List<HistoricVariableInstance> historicVariables = cmmnHistoryService.createHistoricVariableInstanceQuery()
                    .caseInstanceId(caseInstance.getId())
                    .list();
            assertThat(historicVariables).hasSize(2);

            HistoricVariableInstance caseScopeVariable = historicVariables.stream().filter(v -> v.getSubScopeId() == null).findFirst().get();
            assertThat(caseScopeVariable)
                    .extracting(HistoricVariableInstance::getVariableName,
                            HistoricVariableInstance::getVariableTypeName,
                            HistoricVariableInstance::getValue)
                    .containsExactly("aInt", "integer", 5);

            HistoricVariableInstance planItemScopeVariable = historicVariables.stream().filter(v -> v.getSubScopeId() != null).findFirst().get();
            assertThat(planItemScopeVariable)
                    .extracting(HistoricVariableInstance::getVariableName,
                            HistoricVariableInstance::getVariableTypeName,
                            HistoricVariableInstance::getValue)
                    .containsExactly("aString", "string", "value set in the script");
        }
    }

    @Test
    @CmmnDeployment
    public void testSimpleResult() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .start();
        assertThat(caseInstance).isNotNull();

        assertThat(cmmnRuntimeService.hasVariable(caseInstance.getId(), "scriptResult")).isTrue();
        Object result = cmmnRuntimeService.getVariable(caseInstance.getId(), "scriptResult");
        assertThat(result).isInstanceOf(Number.class);
        assertThat(((Number) result).intValue()).isEqualTo(7);

        assertCaseInstanceNotEnded(caseInstance);
        endTestCase();
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testVariableResult() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .variable("a", 3)
                .variable("b", 7)
                .start();
        assertThat(caseInstance).isNotNull();

        assertThat(cmmnRuntimeService.hasVariable(caseInstance.getId(), "scriptResult")).isTrue();
        Object result = cmmnRuntimeService.getVariable(caseInstance.getId(), "scriptResult");
        assertThat(result).isInstanceOf(Number.class);
        assertThat(((Number) result).intValue()).isEqualTo(10);

        assertCaseInstanceNotEnded(caseInstance);
        endTestCase();
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testObjectVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .variable("a", new IntValueHolder(3))
                .variable("b", 7)
                .start();
        assertThat(caseInstance).isNotNull();

        assertThat(cmmnRuntimeService.hasVariable(caseInstance.getId(), "scriptResult")).isTrue();
        Object result = cmmnRuntimeService.getVariable(caseInstance.getId(), "scriptResult");
        assertThat(result).isInstanceOf(Number.class);
        assertThat(((Number) result).intValue()).isEqualTo(12);

        assertThat(cmmnRuntimeService.hasVariable(caseInstance.getId(), "a")).isTrue();
        Object a = cmmnRuntimeService.getVariable(caseInstance.getId(), "a");
        assertThat(a).isInstanceOf(IntValueHolder.class);
        assertThat(((IntValueHolder) a).getValue()).isEqualTo(5);

        assertCaseInstanceNotEnded(caseInstance);
        endTestCase();
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testReturnJavaObject() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .variable("a", new IntValueHolder(3))
                .variable("b", 7)
                .start();
        assertThat(caseInstance).isNotNull();

        assertThat(cmmnRuntimeService.hasVariable(caseInstance.getId(), "scriptResult")).isTrue();
        Object result = cmmnRuntimeService.getVariable(caseInstance.getId(), "scriptResult");
        assertThat(result).isInstanceOf(IntValueHolder.class);
        assertThat(((IntValueHolder) result).getValue()).isEqualTo(10);

        assertCaseInstanceNotEnded(caseInstance);
        endTestCase();
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testGroovyAutoStoreVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .variable("inputArray", new int[] { 1, 2, 3, 4, 5 })
                .start();
        assertThat(caseInstance).isNotNull();

        Map<String, Object> variables = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertThat(variables).hasSize(2);

        assertThat(cmmnRuntimeService.hasVariable(caseInstance.getId(), "sum")).isTrue();
        Object result = cmmnRuntimeService.getVariable(caseInstance.getId(), "sum");
        assertThat(result).isInstanceOf(Integer.class);
        assertThat(((Number) result).intValue()).isEqualTo(15);

        assertCaseInstanceNotEnded(caseInstance);
        endTestCase();
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testGroovyNoAutoStoreVariables() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .variable("inputArray", new int[] { 1, 2, 3, 4, 5 })
                .start();
        assertThat(caseInstance).isNotNull();

        Map<String, Object> variables = cmmnRuntimeService.getVariables(caseInstance.getId());
        assertThat(variables).hasSize(1);

        assertThat(cmmnRuntimeService.hasVariable(caseInstance.getId(), "sum")).isFalse();

        assertCaseInstanceNotEnded(caseInstance);
        endTestCase();
        assertCaseInstanceEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testScriptThrowsFlowableException() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("scriptCase")
                .start();
        assertThat(caseInstance).isNotNull();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItemTaskA").singleResult();
        assertThat(planItemInstance)
                .extracting(PlanItemInstance::getName, PlanItemInstance::getPlanItemDefinitionId)
                .containsExactly("Plan Item One", "taskA");
        assertCaseInstanceNotEnded(caseInstance);

        PlanItemInstance blockerPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("blockerPlanItem").singleResult();
        assertThat(blockerPlanItemInstance).isNotNull();
        assertThatThrownBy(() -> cmmnRuntimeService.triggerPlanItemInstance(blockerPlanItemInstance.getId()))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasNoCause()
                .hasMessage("Illegal argument in script");

        assertCaseInstanceNotEnded(caseInstance);
    }

    /**
     * Tests {@link org.flowable.cmmn.engine.impl.scripting.CmmnEngineScriptTraceEnhancer}
     * together with {@link org.flowable.cmmn.engine.impl.behavior.impl.ScriptTaskActivityBehavior}
     * contributing the expected error trace information
     */
    @Test
    @CmmnDeployment(tenantId = "myTenant")
    public void testScriptThrowsNonFlowableException() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .tenantId("myTenant")
                .caseDefinitionKey("scriptCase")
                .start();
        assertThat(caseInstance).isNotNull();

        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("planItemTaskA").singleResult();
        assertThat(planItemInstance)
                .extracting(PlanItemInstance::getName, PlanItemInstance::getPlanItemDefinitionId)
                .containsExactly("Plan Item One", "taskA");
        assertCaseInstanceNotEnded(caseInstance);

        PlanItemInstance blockerPlanItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("blockerPlanItem").singleResult();
        assertThat(blockerPlanItemInstance).isNotNull();

        String errorMessage =
                "JavaScript script evaluation failed: 'java.lang.RuntimeException: Illegal argument in script in <eval> at line number 2 at column number 28'"
                        + " Trace: scopeType=cmmn, scopeDefinitionKey=scriptCase, scopeDefinitionId=" + planItemInstance.getCaseDefinitionId() + ","
                        + " subScopeDefinitionKey=taskA, tenantId=myTenant, type=scriptTask";
        assertThatThrownBy(() -> cmmnRuntimeService.triggerPlanItemInstance(blockerPlanItemInstance.getId()))
                .isExactlyInstanceOf(FlowableScriptEvaluationException.class)
                .hasMessage(errorMessage)
                .rootCause()
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Illegal argument in script");

        assertCaseInstanceNotEnded(caseInstance);
    }

    @Test
    @CmmnDeployment
    public void testAccessCurrentScopeInformation() {
        CaseInstance caseInstance = cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testAccessCurrentScopeInformation")
                .start();

        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "theCaseInstanceId")).isEqualTo(caseInstance.getId());
        assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "thePlanItemInstanceId")).isNotEqualTo(caseInstance.getId());

        if (CmmnHistoryTestHelper.isHistoryLevelAtLeast(HistoryLevel.ACTIVITY, cmmnEngineConfiguration)) {
            HistoricPlanItemInstance historicPlanItemInstance = cmmnHistoryService.createHistoricPlanItemInstanceQuery()
                    .planItemInstanceDefinitionType(PlanItemDefinitionType.SCRIPT_SERVICE_TASK)
                    .singleResult();

            assertThat(cmmnRuntimeService.getVariable(caseInstance.getId(), "thePlanItemInstanceId")).isEqualTo(historicPlanItemInstance.getId());
        }
    }


    private void endTestCase() {
        PlanItemInstance planItemInstance = cmmnRuntimeService.createPlanItemInstanceQuery().planItemInstanceElementId("blockerPlanItem").singleResult();
        assertThat(planItemInstance).isNotNull();
        cmmnRuntimeService.triggerPlanItemInstance(planItemInstance.getId());
    }

    public static class IntValueHolder implements Serializable {

        private int value;

        public IntValueHolder(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "IntValueHolder{" +
                    "value=" + value +
                    '}';
        }
    }
}
