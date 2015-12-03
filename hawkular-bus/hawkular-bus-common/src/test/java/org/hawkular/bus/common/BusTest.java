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
package org.hawkular.bus.common;

import static java.util.Arrays.asList;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.Queue;
import javax.jms.TextMessage;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.hawkular.bus.common.consumer.BasicMessageListener;
import org.hawkular.bus.common.destinations.JMSQueue;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableMap;

/**
 * @author jsanda
 */
@RunWith(Arquillian.class)
public class BusTest {

    private static final String TEST_QUEUE = "TestQueue";

    private static final String JMS_DELIVERY_COUNTER = "JMSXDeliveryCount";

    private static final String AMQ_LARGE_SIZE = "_AMQ_LARGE_SIZE";

    private static final long DEFAULT_TIMEOUT = 5000;

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class)
                .addPackages(true, "org.hawkular.bus.common")
                .setManifest(new StringAsset("Manifest-Version: 1.0\nDependencies: com.google.guava\n"))
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
//        ZipExporter exporter = new ZipExporterImpl(archive);
//        exporter.exportTo(new File("target", "test-archive.war"));
        return archive;
    }

    @Resource(lookup = "java:/HawkularConnectionFactory")
    ConnectionFactory connectionFactory;

    @Inject
    @JMSQueue("TestQueue")
    Queue testQueue;

    @Inject
    Bus bus;

    @Inject
    MessageSerializer messageSerializer;

    @Inject
    JsonMapper jsonMapper;

    @Before
    public void purgeQueue() throws Exception {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        ObjectName name =
                new ObjectName("jboss.as.expr:jms-queue=TestQueue,subsystem=messaging-activemq,server=default");
        server.invoke(name, "removeMessages", new Object[]{null}, new String[]{String.class.getName()});
    }

    @Test
    public void sendBasicTextMessageToQueue() throws Exception {
        MessageId id = bus.send(testQueue, new BasicTextMessage("test"));

        BasicTextMessage actual = bus.receive(testQueue, DEFAULT_TIMEOUT);
        BasicTextMessage expected = new BasicTextMessage(id, "test");

        assertMessageMatches(expected, actual);
        assertMessageContainsHeaders(actual, JMS_DELIVERY_COUNTER);
    }

    @Test
    public void sendBasicTextMessageWithHeadersToQueue() throws Exception {
        BasicTextMessage message = new BasicTextMessage("test");
        Map<String, String> headers = new HashMap<>();
        headers.put("x", "1");
        headers.put("y", "2");
        message.setHeaders(headers);

        MessageId id = bus.send(testQueue, message);

        BasicTextMessage actual = bus.receive(testQueue, DEFAULT_TIMEOUT);
        BasicTextMessage expected = new BasicTextMessage(id, "test");

        assertMessageMatches(expected, actual);
        assertMessageContainersHeaders(actual, ImmutableMap.of("x", "1", "y", "2"));
        assertMessageContainsHeaders(actual, JMS_DELIVERY_COUNTER);
    }

    @Test
    public void sendMessageWithBinaryData() throws Exception {
        BasicTextMessage message = new BasicTextMessage("test");
        byte[] content = "binary data test".getBytes();
        InputStream inputStream = new ByteArrayInputStream(content);
        message.setBinaryData(inputStream);

        MessageId id = bus.send(testQueue, message);

        BasicTextMessage received = bus.receive(testQueue, DEFAULT_TIMEOUT);
        BasicTextMessage expected = new BasicTextMessage(id, "test");

        assertMessageMatches(expected, received);
        assertMessageContainsHeaders(received, JMS_DELIVERY_COUNTER, AMQ_LARGE_SIZE);

        ByteArrayOutputStream bytesReceived = new ByteArrayOutputStream();
        copy(received.getBinaryData(), bytesReceived);

        assertArrayEquals(content, bytesReceived.toByteArray());
    }

    @Test
    public void sendAndReceiveRPC() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<JMSContext> contextRef = new AtomicReference<>();
        final AtomicReference<Exception> listenerExceptionRef = new AtomicReference<>();
        try {
            contextRef.set(connectionFactory.createContext());
            contextRef.get().createConsumer(testQueue).setMessageListener(message -> {
                try {
                    TextMessage rawRequest = (TextMessage) message;
                    BasicTextMessage receivedRequest = jsonMapper.toBasicMessage(rawRequest.getText(),
                            BasicTextMessage.class);
                    BasicTextMessage response = new BasicTextMessage(receivedRequest.getText().toUpperCase());
                    TextMessage responseMessage = contextRef.get().createTextMessage();
                    responseMessage.setStringProperty(Bus.HEADER_BASIC_MESSAGE_CLASS, BasicTextMessage.class.getName());
                    responseMessage.setText(messageSerializer.toJson(response));
                    Destination replyTo = rawRequest.getJMSReplyTo();
                    contextRef.get().createProducer().send(replyTo, responseMessage);
                    latch.countDown();
                } catch (Exception e) {
                    e.printStackTrace();
                    listenerExceptionRef.set(e);
                    latch.countDown();
                }
            });

            BasicTextMessage request = new BasicTextMessage("test");
            BasicTextMessage response = bus.sendAndReceive(testQueue, request, DEFAULT_TIMEOUT);

            latch.await();

            if (listenerExceptionRef.get() != null) {
                fail("There was an error in the message listener: " + listenerExceptionRef.get().getMessage());
            }

            assertEquals(request.getText().toUpperCase(), response.getText().toUpperCase());
        } finally {
            if (contextRef.get() != null) {
                contextRef.get().close();
            }
        }
    }

    @Test
    public void filterWithMessageSelectors() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("x", "foo");

        BasicTextMessage message1 = new BasicTextMessage("test1");
        message1.setHeaders(headers);

        MessageId id = bus.send(testQueue, message1);

        assertNull(bus.receive(testQueue, "x = 'bar'", 500));

        BasicTextMessage received = bus.receive(testQueue, "x = 'foo'", 500);
        BasicTextMessage expected = new BasicTextMessage(id, "test1");

        assertMessageMatches(expected, received);
        assertMessageContainersHeaders(received, ImmutableMap.of("x", "foo"));
        assertMessageContainsHeaders(received, JMS_DELIVERY_COUNTER);
    }

    @Test
    public void listenForMessages() throws Exception {
        List<String> actual = new ArrayList<>();
        Registration registration = bus.register(testQueue, new BasicMessageListener<BasicTextMessage>() {
            @Override
            public void onBasicMessage(BasicTextMessage basicMessage) {
                actual.add(basicMessage.getText());
            }
        });

        bus.send(testQueue, new BasicTextMessage("A"));
        bus.send(testQueue, new BasicTextMessage("B"));
        bus.send(testQueue, new BasicTextMessage("C"));

        List<String> expected = asList("A", "B", "C");

        Thread.sleep(1000);

        assertEquals(expected, actual);

        // Now cancel the registration and verify no more messages have been received
        registration.cancel();

        bus.send(testQueue, new BasicTextMessage("D"));

        assertEquals(expected, actual);
    }

    private static void copy(InputStream input, OutputStream output) throws IOException {
        final byte[] buffer = new byte[4096];
        int read = 0;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }

        output.flush();
    }

    /**
     * This method compares the {@link BasicTextMessage#getMessageId() messageId},
     * {@link BasicTextMessage#getCorrelationId() correlationId} and the {@link BasicTextMessage#getText() text}
     * properties. It does <strong>not</strong> compare the {@link BasicTextMessage#getHeaders() headers}.
     *
     * @param expected The expected message
     * @param actual The actual message
     * @see #assertMessageContainersHeaders(BasicTextMessage, Map)
     * @see #assertMessageContainsHeaders(BasicTextMessage, String...)
     */
    private void assertMessageMatches(BasicTextMessage expected, BasicTextMessage actual) {
        assertEquals("The messages do not match", expected, actual);
        assertEquals("The message ids do not match", expected.getMessageId(), actual.getMessageId());
        assertEquals("The correlation ids do not match", expected.getCorrelationId(), actual.getCorrelationId());
    }

    /**
     * Verifies that the message contains the specified header names/values.
     *
     * @param message The message to test
     * @param headers The expected headers
     */
    private void assertMessageContainersHeaders(BasicTextMessage message, Map<String, String> headers) {
        headers.entrySet().stream().forEach(entry ->
                assertEquals(entry.getValue(), message.getHeaders().get(entry.getKey())));
    }

    /**
     * Verifie that the message contains the specified header names.
     *
     * @param message The message to test
     * @param headers The header names
     */
    private void assertMessageContainsHeaders(BasicTextMessage message, String... headers) {
        Arrays.stream(headers).forEach(header -> assertTrue(message.getHeaders().containsKey(header)));
    }

}
