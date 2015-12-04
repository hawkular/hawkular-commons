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
import static org.junit.Assert.assertTrue;

import javax.jms.JMSSecurityException;

import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.Endpoint.Type;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.consumer.ConsumerConnectionContext;
import org.hawkular.bus.common.producer.ProducerConnectionContext;
import org.junit.Test;

/**
 * Tests connecting to a secured broker.
 */
public class SecureBrokerTest {
    private static final String USER1_NAME = "user1";
    private static final String USER1_PASSWORD = "user1pw";

    @Test
    public void testLoginFailure() throws Exception {
        ConnectionContextFactory consumerFactory = null;

        SecureTCPEmbeddedBrokerWrapper broker = new SecureTCPEmbeddedBrokerWrapper();
        broker.start();

        try {
            String brokerURL = broker.getBrokerURL();
            consumerFactory = new ConnectionContextFactory(brokerURL); // unauthenticated

            // first check that this fails - we can't connect to a secured broker without logging in
            try {
                consumerFactory.createConsumerConnectionContext(new Endpoint(Type.QUEUE, "testq"));
                assert false : "Should not have been able to connect - we did not authenticate";
            } catch (JMSSecurityException expected) {
                // to be expected
            }
        } finally {
            // close everything
            consumerFactory.close();
            broker.stop();
        }
    }

    @Test
    public void testSecurity() throws Exception {
        ConnectionContextFactory consumerFactory = null;
        ConnectionContextFactory producerFactory = null;

        SecureTCPEmbeddedBrokerWrapper broker = new SecureTCPEmbeddedBrokerWrapper();
        broker.start();

        try {
            String brokerURL = broker.getBrokerURL();
            Endpoint endpoint = new Endpoint(Type.QUEUE, "testq");
            SpecificMessage specificMessage = new SpecificMessage("hello", null, "specific text");

            // mimic server-side
            consumerFactory = new ConnectionContextFactory(brokerURL, USER1_NAME, USER1_PASSWORD);
            ConsumerConnectionContext consumerContext = consumerFactory.createConsumerConnectionContext(endpoint);
            SimpleTestListener<SpecificMessage> listener = new SimpleTestListener<SpecificMessage>(
                    SpecificMessage.class);
            MessageProcessor serverSideProcessor = new MessageProcessor();
            serverSideProcessor.listen(consumerContext, listener);

            // mimic client side
            producerFactory = new ConnectionContextFactory(brokerURL, USER1_NAME, USER1_PASSWORD);
            ProducerConnectionContext producerContext = producerFactory.createProducerConnectionContext(endpoint);
            MessageProcessor clientSideProcessor = new MessageProcessor();

            // send message
            clientSideProcessor.send(producerContext, specificMessage);

            // wait for the message to flow
            assertTrue(listener.waitForMessage(3));
            assertEquals("Should have received the message", listener.getReceivedMessage().getSpecific(),
                    "specific text");

        } finally {
            // close everything
            producerFactory.close();
            consumerFactory.close();
            broker.stop();
        }
    }
}
