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
package org.flowable.cmmn.spring.impl.test;

import org.flowable.cmmn.engine.CmmnEngine;
import org.flowable.cmmn.engine.impl.test.InternalFlowableCmmnExtension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author Filip Hrisafov
 */
public class InternalFlowableCmmnSpringExtension extends InternalFlowableCmmnExtension {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(InternalFlowableCmmnSpringExtension.class);

    @Override
    protected CmmnEngine getCmmnEngine(ExtensionContext context) {
        return getStore(context)
                .getOrComputeIfAbsent(context.getRequiredTestClass(), key -> SpringExtension.getApplicationContext(context).getBean(CmmnEngine.class),
                        CmmnEngine.class);
    }

    @Override
    protected ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(NAMESPACE);
    }
}
