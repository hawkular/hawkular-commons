/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.bus.common.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.Endpoint.Type;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.consumer.ConsumerConnectionContext;
import org.hawkular.bus.common.producer.ProducerConnectionContext;
import org.junit.Test;

/**
 * Tests message selectors and filtering of messagings.
 */
public class MessageSelectorTest {
    @Test
    public void testFilterWithBasicMessageHeaders() throws Exception {
        // this is the same test as testFilter except the headers will be put directly in AbstractMessage
        ConnectionContextFactory consumerFactory = null;
        ConnectionContextFactory producerFactory = null;

        VMEmbeddedBrokerWrapper broker = new VMEmbeddedBrokerWrapper();
        broker.start();

        try {
            String brokerURL = broker.getBrokerURL();
            Endpoint endpoint = new Endpoint(Type.QUEUE, "testq");
            HashMap<String, String> myTestHeaderBoo = new HashMap<String, String>();
            HashMap<String, String> myTestHeaderOther = new HashMap<String, String>();
            myTestHeaderBoo.put("MyTest", "boo");
            myTestHeaderOther.put("MyTest", "Other");

            // mimic server-side
            consumerFactory = new ConnectionContextFactory(brokerURL);
            ConsumerConnectionContext consumerContext = consumerFactory.createConsumerConnectionContext(endpoint,
                    "MyTest = 'boo'");
            SimpleTestListener<SpecificMessage> listener = new SimpleTestListener<SpecificMessage>(
                    SpecificMessage.class);
            MessageProcessor serverSideProcessor = new MessageProcessor();
            serverSideProcessor.listen(consumerContext, listener);

            // mimic client side
            producerFactory = new ConnectionContextFactory(brokerURL);
            ProducerConnectionContext producerContext = producerFactory.createProducerConnectionContext(endpoint);
            MessageProcessor clientSideProcessor = new MessageProcessor();

            // send one that won't match the selector
            SpecificMessage specificMessage = new SpecificMessage("nope", null, "no match");
            specificMessage.setHeaders(myTestHeaderOther);
            clientSideProcessor.send(producerContext, specificMessage);

            // wait for the message to flow - we won't get it because our selector doesn't match
            listener.waitForMessage(3); // 3 seconds is plenty of time to realize we aren't getting it
            assertTrue("Should not have received the message", listener.getReceivedMessage() == null);

            // send one that will match the selector
            specificMessage = new SpecificMessage("hello", null, "specific text");
            specificMessage.setHeaders(myTestHeaderBoo);
            clientSideProcessor.send(producerContext, specificMessage);

            // wait for the message to flow - we should get it now
            listener.waitForMessage(3);
            SpecificMessage receivedMsg = listener.getReceivedMessage();
            assertEquals("Should have received the message", receivedMsg.getSpecific(), "specific text");
            assertNotNull(receivedMsg.getHeaders());
            assertEquals(2, receivedMsg.getHeaders().size());
            assertEquals("boo", receivedMsg.getHeaders().get("MyTest"));
            assertEquals(receivedMsg.getClass().getName(),
                    receivedMsg.getHeaders().get(MessageProcessor.HEADER_BASIC_MESSAGE_CLASS));

        } finally {
            // close everything
            producerFactory.close();
            consumerFactory.close();
            broker.stop();
        }
    }

    @Test
    public void testFilter() throws Exception {
        ConnectionContextFactory consumerFactory = null;
        ConnectionContextFactory producerFactory = null;

        VMEmbeddedBrokerWrapper broker = new VMEmbeddedBrokerWrapper();
        broker.start();

        try {
            String brokerURL = broker.getBrokerURL();
            Endpoint endpoint = new Endpoint(Type.QUEUE, "testq");
            HashMap<String, String> myTestHeaderBoo = new HashMap<String, String>();
            HashMap<String, String> myTestHeaderOther = new HashMap<String, String>();
            myTestHeaderBoo.put("MyTest", "boo");
            myTestHeaderOther.put("MyTest", "Other");

            // mimic server-side
            consumerFactory = new ConnectionContextFactory(brokerURL);
            ConsumerConnectionContext consumerContext = consumerFactory.createConsumerConnectionContext(endpoint,
                    "MyTest = 'boo'");
            SimpleTestListener<SpecificMessage> listener = new SimpleTestListener<SpecificMessage>(
                    SpecificMessage.class);
            MessageProcessor serverSideProcessor = new MessageProcessor();
            serverSideProcessor.listen(consumerContext, listener);

            // mimic client side
            producerFactory = new ConnectionContextFactory(brokerURL);
            ProducerConnectionContext producerContext = producerFactory.createProducerConnectionContext(endpoint);
            MessageProcessor clientSideProcessor = new MessageProcessor();

            // send one that won't match the selector
            SpecificMessage specificMessage = new SpecificMessage("nope", null, "no match");
            clientSideProcessor.send(producerContext, specificMessage, myTestHeaderOther);

            // wait for the message to flow - we won't get it because our selector doesn't match
            listener.waitForMessage(3); // 3 seconds is plenty of time to realize we aren't getting it
            assertTrue("Should not have received the message", listener.getReceivedMessage() == null);

            // send one that will match the selector
            specificMessage = new SpecificMessage("hello", null, "specific text");
            clientSideProcessor.send(producerContext, specificMessage, myTestHeaderBoo);

            // wait for the message to flow - we should get it now
            listener.waitForMessage(3);
            SpecificMessage receivedMsg = listener.getReceivedMessage();
            assertEquals("Should have received the message", receivedMsg.getSpecific(), "specific text");
            assertNotNull(receivedMsg.getHeaders());
            assertEquals(2, receivedMsg.getHeaders().size());
            assertEquals("boo", receivedMsg.getHeaders().get("MyTest"));
            assertEquals(receivedMsg.getClass().getName(),
                    receivedMsg.getHeaders().get(MessageProcessor.HEADER_BASIC_MESSAGE_CLASS));

        } finally {
            // close everything
            producerFactory.close();
            consumerFactory.close();
            broker.stop();
        }
    }
}
