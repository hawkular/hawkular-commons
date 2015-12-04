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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.Endpoint.Type;
import org.hawkular.bus.common.SimpleBasicMessage;
import org.junit.Test;

/**
 * This test class shows usages of the different Embedded Broker Wrapper objects
 * as well as the convenience connections for both consumer and producer.
 */
public class EmbeddedBrokerTest {
    @Test
    public void testInternalVMBrokerQueue() throws Exception {
        internalTestBroker(new VMEmbeddedBrokerWrapper(), new Endpoint(Type.QUEUE, "testq"));
    }

    @Test
    public void testInternalVMBrokerTopic() throws Exception {
        internalTestBroker(new VMEmbeddedBrokerWrapper(), new Endpoint(Type.TOPIC, "testtopic"));
    }

    @Test
    public void testTCPBrokerQueue() throws Exception {
        internalTestBroker(new TCPEmbeddedBrokerWrapper(), new Endpoint(Type.QUEUE, "testq"));
    }

    @Test
    public void testTCPBrokerTopic() throws Exception {
        internalTestBroker(new TCPEmbeddedBrokerWrapper(), new Endpoint(Type.TOPIC, "testtopic"));
    }

    private void internalTestBroker(AbstractEmbeddedBrokerWrapper broker, Endpoint endpoint) throws Exception {
        broker.start();
        assert broker.getBroker().isBrokerStarted() : "Broker should have been started by now";

        try {
            String brokerURL = broker.getBrokerURL();

            // test that messages can flow to the given broker
            Map<String, String> details = new HashMap<String, String>();
            details.put("key1", "val1");
            details.put("secondkey", "secondval");
            SimpleBasicMessage basicMessage = new SimpleBasicMessage("Hello World!", details);

            CountDownLatch latch = new CountDownLatch(1);
            ArrayList<SimpleBasicMessage> receivedMessages = new ArrayList<SimpleBasicMessage>();
            ArrayList<String> errors = new ArrayList<String>();

            // start the consumer
            StoreAndLatchBasicMessageListener<SimpleBasicMessage> messageListener;
            messageListener = new StoreAndLatchBasicMessageListener<SimpleBasicMessage>(latch, receivedMessages,
                    errors, SimpleBasicMessage.class);
            ConsumerConnection consumerConnection = new ConsumerConnection(brokerURL, endpoint, messageListener);

            // start the producer
            ProducerConnection producerConnection = new ProducerConnection(brokerURL, endpoint);
            producerConnection.sendMessage(basicMessage.toJSON());

            // wait for the message to flow
            boolean gotMessage = latch.await(5, TimeUnit.SECONDS);
            if (!gotMessage) {
                errors.add("Timed out waiting for message - it never showed up");
            }

            // close everything
            producerConnection.close();
            consumerConnection.close();

            // make sure the message flowed properly
            assertTrue("Failed to send message properly: " + errors, errors.isEmpty());
            assertEquals("Didn't receive message: " + receivedMessages, 1, receivedMessages.size());
            SimpleBasicMessage receivedBasicMessage = receivedMessages.get(0);
            assertEquals(basicMessage.getMessage(), receivedBasicMessage.getMessage());
            assertEquals(basicMessage.getDetails(), receivedBasicMessage.getDetails());
        } finally {
            broker.stop();
        }
    }

    @Test
    public void testSubClassingBasicMessage() throws Exception {
        VMEmbeddedBrokerWrapper broker = new VMEmbeddedBrokerWrapper();
        assert !broker.getBroker().isBrokerStarted() : "Broker should not have been started yet";
        broker.start();
        assert broker.getBroker().isBrokerStarted() : "Broker should have been started by now";

        try {
            String brokerURL = broker.getBrokerURL();
            Endpoint endpoint = new Endpoint(Type.QUEUE, "testq");

            // test that sending messages of a AbstractMessage subclass type can flow
            Map<String, String> details = new HashMap<String, String>();
            details.put("key1", "val1");
            details.put("secondkey", "secondval");
            SpecificMessage specificMessage = new SpecificMessage("hello", details, "specific text");

            CountDownLatch latch = new CountDownLatch(1);
            ArrayList<SpecificMessage> receivedMessages = new ArrayList<SpecificMessage>();
            ArrayList<String> errors = new ArrayList<String>();

            // start the consumer listening for our subclass SpecificMessage objects
            StoreAndLatchBasicMessageListener<SpecificMessage> messageListener;
            messageListener = new StoreAndLatchBasicMessageListener<SpecificMessage>(latch, receivedMessages, errors,
                    SpecificMessage.class);
            ConsumerConnection consumerConnection = new ConsumerConnection(brokerURL, endpoint, messageListener);

            // start the producer
            ProducerConnection producerConnection = new ProducerConnection(brokerURL, endpoint);
            producerConnection.sendMessage(specificMessage.toJSON());

            // wait for the message to flow
            boolean gotMessage = latch.await(5, TimeUnit.SECONDS);
            if (!gotMessage) {
                errors.add("Timed out waiting for message - it never showed up");
            }

            // close everything
            producerConnection.close();
            consumerConnection.close();

            // make sure the message flowed properly
            assertTrue("Failed to send message properly: " + errors, errors.isEmpty());
            assertEquals("Didn't receive message: " + receivedMessages, 1, receivedMessages.size());
            SpecificMessage receivedSpecificMessage = receivedMessages.get(0);
            assertEquals(specificMessage.getMessage(), receivedSpecificMessage.getMessage());
            assertEquals(specificMessage.getDetails(), receivedSpecificMessage.getDetails());
            assertEquals(specificMessage.getSpecific(), receivedSpecificMessage.getSpecific());
        } finally {
            broker.stop();
        }
    }

    @Test
    public void testSubClassingBasicMessageAndListener() throws Exception {
        VMEmbeddedBrokerWrapper broker = new VMEmbeddedBrokerWrapper();
        broker.start();

        try {
            String brokerURL = broker.getBrokerURL();
            Endpoint endpoint = new Endpoint(Type.QUEUE, "testq");

            // test that sending messages of a AbstractMessage subclass type can flow
            Map<String, String> details = new HashMap<String, String>();
            details.put("key1", "val1");
            details.put("secondkey", "secondval");
            SpecificMessage specificMessage = new SpecificMessage("hello", details, "specific text");

            CountDownLatch latch = new CountDownLatch(1);
            ArrayList<SpecificMessage> receivedMessages = new ArrayList<SpecificMessage>();
            ArrayList<String> errors = new ArrayList<String>();

            // start the consumer listening for our subclass SpecificMessage objects
            SpecificMessageStoreAndLatchListener messageListener = new SpecificMessageStoreAndLatchListener(latch,
                    receivedMessages, errors);
            ConsumerConnection consumerConnection = new ConsumerConnection(brokerURL, endpoint, messageListener);

            // start the producer
            ProducerConnection producerConnection = new ProducerConnection(brokerURL, endpoint);
            producerConnection.sendMessage(specificMessage.toJSON());

            // wait for the message to flow
            boolean gotMessage = latch.await(5, TimeUnit.SECONDS);
            if (!gotMessage) {
                errors.add("Timed out waiting for message - it never showed up");
            }

            // close everything
            producerConnection.close();
            consumerConnection.close();

            // make sure the message flowed properly
            assertTrue("Failed to send message properly: " + errors, errors.isEmpty());
            assertEquals("Didn't receive message: " + receivedMessages, 1, receivedMessages.size());
            SpecificMessage receivedSpecificMessage = receivedMessages.get(0);
            assertEquals(specificMessage.getMessage(), receivedSpecificMessage.getMessage());
            assertEquals(specificMessage.getDetails(), receivedSpecificMessage.getDetails());
            assertEquals(specificMessage.getSpecific(), receivedSpecificMessage.getSpecific());
        } finally {
            broker.stop();
        }
    }
}
