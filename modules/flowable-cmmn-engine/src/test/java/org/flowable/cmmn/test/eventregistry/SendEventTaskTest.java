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
package org.flowable.cmmn.test.eventregistry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.flowable.cmmn.engine.test.CmmnDeployment;
import org.flowable.eventregistry.api.EventDeployment;
import org.flowable.eventregistry.api.EventRepositoryService;
import org.flowable.eventregistry.api.OutboundEventChannelAdapter;
import org.flowable.eventregistry.api.model.EventPayloadTypes;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Joram Barrez
 */
public class SendEventTaskTest extends FlowableEventRegistryCmmnTestCase {

    protected TestOutboundEventChannelAdapter outboundEventChannelAdapter;

    @BeforeEach
    public void registerEventDefinition() {
        outboundEventChannelAdapter = setupTestChannel();

        getEventRepositoryService().createEventModelBuilder()
                .key("testEvent")
                .resourceName("testEvent.event")
                .header("headerProperty1", EventPayloadTypes.STRING)
                .header("headerProperty2", EventPayloadTypes.STRING)
                .payload("customerId", EventPayloadTypes.STRING)
                .deploy();
    }

    protected TestOutboundEventChannelAdapter setupTestChannel() {
        TestOutboundEventChannelAdapter outboundEventChannelAdapter = new TestOutboundEventChannelAdapter();
        getEventRegistryEngineConfiguration().getExpressionManager().getBeans()
                .put("outboundEventChannelAdapter", outboundEventChannelAdapter);

        getEventRepositoryService().createOutboundChannelModelBuilder()
                .key("out-channel")
                .resourceName("out.channel")
                .channelAdapter("${outboundEventChannelAdapter}")
                .jsonSerializer()
                .deploy();

        return outboundEventChannelAdapter;
    }

    @AfterEach
    public void unregisterEventDefinition() {
        EventRepositoryService eventRepositoryService = getEventRepositoryService();
        List<EventDeployment> deployments = eventRepositoryService.createDeploymentQuery().list();
        for (EventDeployment eventDeployment : deployments) {
            eventRepositoryService.deleteDeployment(eventDeployment.getId());
        }
    }

    @Test
    @CmmnDeployment
    public void testSimpleSendEvent() throws Exception {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testSendEvent")
                .variable("myVariable", "Hello World!")
                .start();

        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(1);

        JsonNode jsonNode = cmmnEngineConfiguration.getObjectMapper().readTree(outboundEventChannelAdapter.receivedEvents.get(0));
        assertThat(jsonNode).hasSize(1);
        assertThat(jsonNode.get("customerId").asText()).isEqualTo("Hello World!");
    }
    
    @Test
    @CmmnDeployment
    public void testEventWithHeaders() throws Exception {
        cmmnRuntimeService.createCaseInstanceBuilder()
                .caseDefinitionKey("testSendEvent")
                .variable("myHeaderValue", "My header value")
                .variable("myVariable", "Hello World!")
                .start();

        assertThat(outboundEventChannelAdapter.receivedEvents).hasSize(1);

        JsonNode jsonNode = cmmnEngineConfiguration.getObjectMapper().readTree(outboundEventChannelAdapter.receivedEvents.get(0));
        assertThat(jsonNode).hasSize(1);
        assertThat(jsonNode.get("customerId").asText()).isEqualTo("Hello World!");
        
        Map<String, Object> headerMap = outboundEventChannelAdapter.headers.get(0);
        assertThat(headerMap.get("headerProperty1")).isEqualTo("test");
        assertThat(headerMap.get("headerProperty2")).isEqualTo("My header value");
    }

    public static class TestOutboundEventChannelAdapter implements OutboundEventChannelAdapter<String> {

        public List<String> receivedEvents = new ArrayList<>();
        public List<Map<String, Object>> headers = new ArrayList<>();

        @Override
        public void sendEvent(String rawEvent, Map<String, Object> headerMap) {
            receivedEvents.add(rawEvent);
            headers.add(headerMap);
        }
    }
}
