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

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.Endpoint.Type;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.consumer.ConsumerConnectionContext;
import org.hawkular.bus.common.producer.ProducerConnectionContext;
import org.junit.Test;

/**
 * Tests the feature of listeners being able to change the name of the POJO that deserializes incoming JSON.
 */
public class ChangeMessageClassNameTest {

    @Test
    public void testChangeClassName() throws Exception {
        ConnectionContextFactory consumerFactory = null;
        ConnectionContextFactory producerFactory = null;

        VMEmbeddedBrokerWrapper broker = new VMEmbeddedBrokerWrapper();
        broker.start();

        try {
            String brokerURL = broker.getBrokerURL();
            Endpoint endpoint = new Endpoint(Type.QUEUE, "testq");

            // mimic server-side
            consumerFactory = new ConnectionContextFactory(brokerURL);
            ConsumerConnectionContext consumerContext = consumerFactory.createConsumerConnectionContext(endpoint);
            CountDownLatch latch = new CountDownLatch(1);
            ArrayList<SpecificMessageDuplicate> messages = new ArrayList<>();
            ArrayList<String> errors = new ArrayList<String>();
            SpecificMessageDuplicateListener listener = new SpecificMessageDuplicateListener(latch, messages, errors);
            MessageProcessor serverSideProcessor = new MessageProcessor();
            serverSideProcessor.listen(consumerContext, listener);

            // mimic client side
            producerFactory = new ConnectionContextFactory(brokerURL);
            ProducerConnectionContext producerContext = producerFactory.createProducerConnectionContext(endpoint);
            MessageProcessor clientSideProcessor = new MessageProcessor();

            // NOTE: sending SpecificMessage - but our listener was declared to want SpecificMessageDuplicate
            //       this is the core of the test - we want to see the listener be able to accept a SpecificMessage
            //       and use SpecificMessageDuplicate instead when deserializing SpecificMessage's JSON
            SpecificMessage specificMessage = new SpecificMessage("the message", null, "specific data");
            clientSideProcessor.send(producerContext, specificMessage);

            // wait for the message to flow - we won't get it because our selector doesn't match
            listener.getLatch().await(3, TimeUnit.SECONDS);
            assertEquals("Should have received the message", 1, listener.getMessages().size());

            assertEquals(SpecificMessageDuplicate.class, listener.getMessages().get(0).getClass());
            SpecificMessageDuplicate receivedMsg = listener.getMessages().get(0);
            assertEquals("Should have received the message", receivedMsg.getSpecific(), "specific data");
            assertEquals(
                    "Should have been told it was a SpecificMessage, even though we used SpecificMessageDuplicate",
                    SpecificMessage.class.getName(),
                    receivedMsg.getHeaders().get(MessageProcessor.HEADER_BASIC_MESSAGE_CLASS));

        } finally {
            // close everything
            producerFactory.close();
            consumerFactory.close();
            broker.stop();
        }
    }
}
