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
package org.flowable.dmn.engine.impl.el;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.joda.JodaDeprecationLogger;
import org.flowable.dmn.api.ExecuteDecisionContext;
import org.flowable.dmn.engine.impl.audit.DecisionExecutionAuditUtil;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionService;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.OutputClause;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yvo Swillens
 */
public class ELExecutionContextBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(ELExecutionContextBuilder.class);

    public static ELExecutionContext build(DecisionService decisionService, ExecuteDecisionContext executeDecisionInfo) {
        ELExecutionContext executionContext = new ELExecutionContext();
        executionContext.setInstanceId(executeDecisionInfo.getInstanceId());
        executionContext.setScopeType(executeDecisionInfo.getScopeType());
        executionContext.setTenantId(executeDecisionInfo.getTenantId());

        executionContext.setAuditContainer(DecisionExecutionAuditUtil.initializeDecisionServiceExecutionAudit(decisionService, executeDecisionInfo));

        return executionContext;
    }

    public static ELExecutionContext build(Decision decision, ExecuteDecisionContext executeDecisionInfo) {
        ELExecutionContext executionContext = new ELExecutionContext();
        executionContext.setInstanceId(executeDecisionInfo.getInstanceId());
        executionContext.setScopeType(executeDecisionInfo.getScopeType());
        executionContext.setTenantId(executeDecisionInfo.getTenantId());
        executionContext.setForceDMN11(decision.isForceDMN11());

        // initialize audit trail
        executionContext.setAuditContainer(DecisionExecutionAuditUtil.initializeDecisionExecutionAudit(decision, executeDecisionInfo));

        DecisionTable decisionTable = (DecisionTable) decision.getExpression();

        // add output values to context
        if (decisionTable.getOutputs() != null) {
            for (OutputClause outputClause : decisionTable.getOutputs()) {
                if (outputClause.getOutputValues() != null && outputClause.getOutputValues().getTextValues() != null) {
                    executionContext.addOutputValues(outputClause.getName(),
                        ExecutionVariableFactory.getExecutionVariables(outputClause.getTypeRef(), outputClause.getOutputValues().getTextValues()));
                }
            }
        }

        // set aggregator
        if (decisionTable.getAggregation() != null) {
            executionContext.setAggregator(decisionTable.getAggregation());
        }

        Map<String, Object> inputVariables = executeDecisionInfo.getVariables();
        preProcessInputVariables(decisionTable, inputVariables, executeDecisionInfo);
        executionContext.setStackVariables(inputVariables);

        LOGGER.debug("Execution Context created");

        return executionContext;
    }

    protected static void preProcessInputVariables(DecisionTable decisionTable, Map<String, Object> inputVariables, ExecuteDecisionContext executeDecisionInfo) {

        if (inputVariables == null) {
            inputVariables = new HashMap<>();
        }

        // check if there are input expressions that refer to none existing input variables
        // that need special handling
        for (InputClause inputClause : decisionTable.getInputs()) {
            if (!inputVariables.containsKey(inputClause.getInputExpression().getText()) && "boolean".equals(inputClause.getInputExpression().getTypeRef())) {
                inputVariables.put(inputClause.getInputExpression().getText(), Boolean.FALSE);
            }
        }

        // check if there are output expressions that refer to none existing input variables
        // in that case create them with default values
        for (OutputClause outputClause : decisionTable.getOutputs()) {
            if (!inputVariables.containsKey(outputClause.getName()) || inputVariables.get(outputClause.getName()) == null) {
                if ("number".equals(outputClause.getTypeRef())) {
                    inputVariables.put(outputClause.getName(), 0D);
                } else if ("date".equals(outputClause.getTypeRef())) {
                    inputVariables.put(outputClause.getName(), new Date());
                } else {
                    inputVariables.put(outputClause.getName(), "");
                }
            }
        }

        // check if transformation is needed
        for (Map.Entry<String, Object> inputVariable : inputVariables.entrySet()) {
            String inputVariableName = inputVariable.getKey();
            try {
                Object inputVariableValue = inputVariable.getValue();
                if (inputVariableValue instanceof LocalDate) {
                    JodaDeprecationLogger.LOGGER.warn(
                            "Using Joda-Time LocalDate has been deprecated and will be removed in a future version."
                                    + " Input variable {} from {} {} for decision table {} is a Joda-Time LocalDate. ",
                            inputVariableName, executeDecisionInfo.getScopeType(), executeDecisionInfo.getInstanceId(), decisionTable.getId());
                    Date transformedDate = ((LocalDate) inputVariableValue).toDate();
                    inputVariables.put(inputVariableName, transformedDate);
                } else if (inputVariableValue instanceof java.time.LocalDate) {
                    Date transformedDate = Date.from(((java.time.LocalDate) inputVariableValue).atStartOfDay()
                            .atZone(ZoneId.systemDefault())
                            .toInstant());
                    inputVariables.put(inputVariableName, transformedDate);
                } else if (inputVariableValue instanceof Long || inputVariableValue instanceof Integer) {
                    BigInteger transformedNumber = new BigInteger(inputVariableValue.toString());
                    inputVariables.put(inputVariableName, transformedNumber);
                } else if (inputVariableValue instanceof Double ) {
                    BigDecimal transformedNumber = new BigDecimal((Double) inputVariableValue);
                    inputVariables.put(inputVariableName, transformedNumber);
                } else if (inputVariableValue instanceof Float) {
                    double doubleValue = Double.parseDouble(inputVariableValue.toString());
                    BigDecimal transformedNumber = new BigDecimal(doubleValue);
                    inputVariables.put(inputVariableName, transformedNumber);
                }
            } catch (Exception ex) {
                throw new FlowableException("error while transforming input variable " + inputVariableName + " for decision table " + decisionTable.getId(), ex);
            }
        }
    }
}
