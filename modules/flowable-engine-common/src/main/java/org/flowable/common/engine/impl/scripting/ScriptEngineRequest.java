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
package org.flowable.common.engine.impl.scripting;

import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;

import org.flowable.common.engine.api.FlowableIllegalStateException;
import org.flowable.common.engine.api.variable.VariableContainer;

/**
 * Request to execute a script in the scripting environment.
 * Use {@link ScriptEngineRequest#builder()} to create and configure instances.
 */
public class ScriptEngineRequest {

    protected final String language;
    protected final String script;
    protected final VariableContainer scopeContainer;
    protected final VariableContainer inputVariableContainer;
    protected final List<Resolver> additionalResolvers;
    protected final boolean storeScriptVariables;
    protected final ScriptTraceEnhancer traceEnhancer;

    /**
     * @return a new Builder instance to create a {@link ScriptEngineRequest}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ScriptEngineRequest}.
     */
    public static class Builder {

        protected String language;
        protected String script;
        protected VariableContainer scopeContainer;
        protected VariableContainer inputVariableContainer;
        protected List<Resolver> additionalResolvers = new LinkedList<>();
        protected boolean storeScriptVariables;
        protected ScriptTraceEnhancer traceEnhancer;

        protected Builder() {
        }

        /**
         * The script content for the given language.
         */
        public Builder script(String script) {
            this.script = script;
            return this;
        }

        /**
         * The script language for the script.
         */
        public Builder language(String language) {
            this.language = language;
            return this;
        }

        /**
         * The scope variable container in which the script is being executed in.
         * Used to create {@link Resolver}s for the script context.
         *
         * The variable container will be passed to {@link ResolverFactory} to create specialized Resolvers
         * for the specific VariableContainer implementations.
         * @see #inputVariableContainer(VariableContainer)
         */
        public Builder scopeContainer(VariableContainer variableContainer) {
            this.scopeContainer = variableContainer;
            return this;
        }

        /**
         * The variable container that can be used to provide different dynamic variables for the script context.
         * If not provided then the {@link #scopeContainer} will be used.
         * <p>
         *     e.g. if we have the following script <code>var sum = a + b</code>.
         *     When <code>inputVariableContainer</code> is defined the variables <code>a</code> and <code>b</code> will come from it.
         *     Otherwise, they will come from the variable container defined in {@link #scopeContainer(VariableContainer)}.
         *  <p>
         * The variable container will be passed to {@link ResolverFactory} to create specialized Resolvers
         * for the specific VariableContainer implementations.
         */
        public Builder inputVariableContainer(VariableContainer variableContainer) {
            this.inputVariableContainer = variableContainer;
            return this;
        }

        /**
         * Automatically store variables from script evaluation context
         * to the given variable container. Not recommended, to avoid variableContainer pollution.
         * Better to put the script evaluation result object to the variableContainer, if required.
         */
        public Builder storeScriptVariables() {
            this.storeScriptVariables = true;
            return this;
        }

        /**
         * Adds additional resolver to the end of the list of resolvers.
         * The order of the resolvers matter, as the first resolver returning containsKey = true
         * will be used to resolve a variable during script execution.
         * The resolvers take precedence over the resolvers created for the {@link #scopeContainer}.
         * Useful to provide context objects to the scripting environment.
         */
        public Builder additionalResolver(Resolver additionalResolver) {
            this.additionalResolvers.add(additionalResolver);
            return this;
        }

        /**
         * Configure an {@link ScriptTraceEnhancer}
         * which is called, when a ScriptTrace is created.
         * Allows to provide additional context information for a script trace by allow to "tag"
         * the script invocation with additional meta information.
         * Script traces are produced in case of errors and/or when a {@link ScriptTraceListener} is configured.
         */
        public Builder traceEnhancer(ScriptTraceEnhancer enhancer) {
            this.traceEnhancer = enhancer;
            return this;
        }

        public ScriptEngineRequest build() {
            if (script == null || script.isEmpty()) {
                throw new FlowableIllegalStateException("A script is required");
            }
            return new ScriptEngineRequest(script,
                    language,
                    scopeContainer,
                    inputVariableContainer != null ? inputVariableContainer : scopeContainer,
                    storeScriptVariables,
                    additionalResolvers,
                    traceEnhancer);
        }
    }

    private ScriptEngineRequest(String script,
            String language,
            VariableContainer scopeContainer,
            VariableContainer inputVariableContainer,
            boolean storeScriptVariables,
            List<Resolver> additionalResolvers,
            ScriptTraceEnhancer errorTraceEnhancer) {
        this.script = script;
        this.language = language;
        this.scopeContainer = scopeContainer;
        this.inputVariableContainer = inputVariableContainer;
        this.storeScriptVariables = storeScriptVariables;
        this.additionalResolvers = additionalResolvers;
        this.traceEnhancer = errorTraceEnhancer;
    }

    /**
     * @see Builder#(String)
     */
    public String getLanguage() {
        return language;
    }

    /**
     * @see Builder#(String)
     */
    public String getScript() {
        return script;
    }

    /**
     * @see Builder#scopeContainer(VariableContainer)
     */
    public VariableContainer getScopeContainer() {
        return scopeContainer;
    }

    /**
     * @see Builder#inputVariableContainer(VariableContainer)
     */
    public VariableContainer getInputVariableContainer() {
        return inputVariableContainer;
    }

    /**
     * @see Builder#storeScriptVariables
     */
    public boolean isStoreScriptVariables() {
        return storeScriptVariables;
    }

    /**
     * @see Builder#additionalResolver(Resolver)
     */
    public List<Resolver> getAdditionalResolvers() {
        return additionalResolvers;
    }


    /**
     * @see Builder#traceEnhancer(ScriptTraceEnhancer)
     */
    public ScriptTraceEnhancer getTraceEnhancer() {
        return traceEnhancer;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ScriptEngineRequest.class.getSimpleName() + "[", "]")
                .add("language='" + language + "'")
                .add("script='" + script + "'")
                .add("variableContainer=" + scopeContainer)
                .add("storeScriptVariables=" + storeScriptVariables)
                .toString();
    }
}
