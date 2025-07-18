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
package org.flowable.dmn.engine.test.runtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.dmn.api.DmnDeployment;
import org.flowable.dmn.engine.test.BaseFlowableDmnTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This class tests fallbacks in {@link org.flowable.dmn.engine.impl.cmd.AbstractExecuteDecisionCmd}
 */
class DecisionTableExecutionFallBackTest extends BaseFlowableDmnTest {

    public static final String TEST_TENANT_ID = "testTenantId";
    public static final String TEST_PARENT_DEPLOYMENT_ID = "testParentDeploymentId";

    protected DmnDeployment deployment;

    @BeforeEach
    public void createDeployment() {
        deployment = dmnEngine.getDmnRepositoryService().createDeployment().
                addClasspathResource("org/flowable/dmn/engine/test/runtime/StandaloneRuntimeTest.ruleUsageExample.dmn").
                tenantId(TEST_TENANT_ID).
                parentDeploymentId(TEST_PARENT_DEPLOYMENT_ID).
                deploy();
    }

    @AfterEach
    public void cleanUpDeployment() {
        dmnEngine.getDmnRepositoryService().deleteDeployment(deployment.getId());
    }

    @Test
    public void decisionKeyDeploymentIdTenantId() {
        Map<String, Object> result = executeDecision(TEST_TENANT_ID, TEST_PARENT_DEPLOYMENT_ID);
        assertThat(result).containsEntry("outputVariable1", "result2");
    }


    @Test
    public void fallBackDecisionKeyDeploymentIdTenantIdWrongDeploymentId() {
        Map<String, Object> result = executeDecision(TEST_TENANT_ID, "WRONG_PARENT_DEPLOYMENT_ID");

        assertThat(result).containsEntry("outputVariable1", "result2");
    }

    @Test
    public void decisionKeyDeploymentIdTenantIdWrongTenantIdThrowsException() {
        assertThatThrownBy(() -> executeDecision("WRONG_TENANT_ID", TEST_PARENT_DEPLOYMENT_ID))
                .isInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessageContaining("No decision found for key: decision1, parent deployment id testParentDeploymentId and tenant id: WRONG_TENANT_ID.");
    }

    @Test
    public void decisionKeyTenantIdWrongTenantIdThrowsException() {
        assertThatThrownBy(() -> executeDecision("WRONG_TENANT_ID", null))
                .isInstanceOf(FlowableObjectNotFoundException.class)
                .hasMessage("No decision found for key: decision1 and tenantId: WRONG_TENANT_ID.");
    }

    @Test
    public void decisionKeyDeploymentId() {
        DmnDeployment localDeployment = dmnEngine.getDmnRepositoryService().createDeployment().
                addClasspathResource("org/flowable/dmn/engine/test/runtime/StandaloneRuntimeTest.ruleUsageExample.dmn").
                tenantId(null).
                parentDeploymentId(TEST_PARENT_DEPLOYMENT_ID).
                deploy();
        try {
            Map<String, Object> result = executeDecision(null, TEST_PARENT_DEPLOYMENT_ID);

            assertThat(result).containsEntry("outputVariable1", "result2");
        } finally {
            dmnEngine.getDmnRepositoryService().deleteDeployment(localDeployment.getId());
        }
    }

    @Test
    public void decisionKeyTenantId() {
        Map<String, Object> result = executeDecision(TEST_TENANT_ID, null);
        assertThat(result).containsEntry("outputVariable1", "result2");
    }


    @Test
    public void fallBackDecisionKeyDeploymentId_wrongDeploymentId() {
        DmnDeployment localDeployment = dmnEngine.getDmnRepositoryService().createDeployment().
                addClasspathResource("org/flowable/dmn/engine/test/runtime/StandaloneRuntimeTest.ruleUsageExample.dmn").
                tenantId(null).
                parentDeploymentId(TEST_PARENT_DEPLOYMENT_ID).
                deploy();
        try {
            Map<String, Object> result = executeDecision(null, "WRONG_PARENT_DEPLOYMENT_ID");

            assertThat(result).containsEntry("outputVariable1", "result2");
        } finally {
            dmnEngine.getDmnRepositoryService().deleteDeployment(localDeployment.getId());
        }
    }

    @Test
    public void fallBackDecisionKeyDeploymentId_fallbackToDefaultTenant() {
        DmnDeployment localDeployment = dmnEngine.getDmnRepositoryService().createDeployment().
                addClasspathResource("org/flowable/dmn/engine/test/runtime/StandaloneRuntimeTest.ruleUsageExample.dmn").
                tenantId(null).
                parentDeploymentId(TEST_PARENT_DEPLOYMENT_ID).
                deploy();
        try {
            Map<String, Object> inputVariables = new HashMap<>();
            inputVariables.put("inputVariable1", 2);
            inputVariables.put("inputVariable2", "test2");

            Map<String, Object> result = dmnEngine.getDmnDecisionService().createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .tenantId("flowable")
                .parentDeploymentId(localDeployment.getId())
                .variables(inputVariables)
                .fallbackToDefaultTenant()
                .executeWithSingleResult();

            assertThat(result).containsEntry("outputVariable1", "result2");
        } finally {
            dmnEngine.getDmnRepositoryService().deleteDeployment(localDeployment.getId());
        }
    }

    protected Map<String, Object> executeDecision(String tenantId, String parentDeploymentId) {
        Map<String, Object> inputVariables = new HashMap<>();
        inputVariables.put("inputVariable1", 2);
        inputVariables.put("inputVariable2", "test2");

        return dmnEngine.getDmnDecisionService().createExecuteDecisionBuilder()
                .decisionKey("decision1")
                .tenantId(tenantId)
                .parentDeploymentId(parentDeploymentId)
                .variables(inputVariables)
                .executeWithSingleResult();
    }

}
