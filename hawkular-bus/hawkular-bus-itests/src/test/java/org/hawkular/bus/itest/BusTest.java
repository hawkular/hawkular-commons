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
package org.hawkular.bus.itest;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import org.hawkular.bus.common.BasicMessage;
import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.hawkular.bus.common.BinaryData;
import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.Endpoint.Type;
import org.hawkular.bus.common.MessageId;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.SimpleBasicMessage;
import org.hawkular.bus.common.consumer.BasicMessageListener;
import org.hawkular.bus.common.consumer.ConsumerConnectionContext;
import org.hawkular.bus.common.producer.ProducerConnectionContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author jsanda
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 */
@RunWith(Arquillian.class)
public class BusTest {
    private static class MessageReceiver implements Closeable {

        private class MessageListener extends BasicMessageListener<BasicMessage> {

            public MessageListener() {
                super(BusTest.class.getClassLoader());
            }

            @Override
            protected void onBasicMessage(BasicMessageWithExtraData<BasicMessage> messageWithData) {
                final BasicMessage basicMessage = messageWithData.getBasicMessage();
                log.infof("Received message [%s] with binary data [%b]", basicMessage.getClass().getName(),
                        messageWithData.getBinaryData() != null);
                MessageReceiver.this.receivedMessages.add(messageWithData);
            }
        }

        private final ConnectionContextFactory connectionContextFactory;
        private final ConsumerConnectionContext consumerConnectionContext;
        private final MessageListener messageListener = new MessageListener();
        private BlockingQueue<BasicMessageWithExtraData<BasicMessage>> receivedMessages = new ArrayBlockingQueue<>(1);

        public MessageReceiver(ConnectionFactory connectionFactory, Endpoint endpoint)
                throws JMSException, IOException {
            String messageSelector = String.format("%s = '%s'", TEST_HEADER, TEST_VALUE);
            this.connectionContextFactory = new ConnectionContextFactory(true, connectionFactory);
            this.consumerConnectionContext = connectionContextFactory.createConsumerConnectionContext(endpoint,
                    messageSelector);
            new MessageProcessor().listen(consumerConnectionContext, messageListener);

        }

        /**
         * @see java.io.Closeable#close()
         */
        @Override
        public void close() throws IOException {
            if (consumerConnectionContext != null) {
                try {
                    consumerConnectionContext.close();
                } catch (IOException e) {
                    log.errorf(e, "Could not close [%s]", consumerConnectionContext.getClass().getName());
                }
            }

            if (connectionContextFactory != null) {
                try {
                    connectionContextFactory.close();
                } catch (Exception e) {
                    log.errorf(e, "Could not close [%s]", connectionContextFactory.getClass().getName());
                }
            }
        }

    }

    private static final Logger log = Logger.getLogger(BusTest.class);

    private static final String TEST_HEADER = "testHeader";
    private static final String TEST_QUEUE = "TestQueue";
    private static final String TEST_VALUE = "testValue";

    public static void copy(InputStream input, OutputStream output) throws IOException {
        final byte[] buffer = new byte[4096];
        int read = 0;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }

        output.flush();
    }

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(BusTest.class.getResource("/jboss-deployment-structure.xml"),
                        "jboss-deployment-structure.xml")
                .addPackage(BusTest.class.getPackage());
        ZipExporter exporter = new ZipExporterImpl(archive);
        exporter.exportTo(new File("target", "test-archive.war"));

        return archive;
    }

    @Resource(name = "java:/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Test
    public void textMessage() throws JMSException, IOException, InterruptedException {
        assertNotNull(connectionFactory);

        Endpoint endpoint = new Endpoint(Type.QUEUE, TEST_QUEUE);

        try (MessageReceiver receiver = new MessageReceiver(connectionFactory, endpoint)) {
            SimpleBasicMessage messageSent = new SimpleBasicMessage("textMessage");
            BasicMessageWithExtraData<SimpleBasicMessage> envelopeSent =
                    new BasicMessageWithExtraData<SimpleBasicMessage>(messageSent, null);
            try (ConnectionContextFactory ccf = new ConnectionContextFactory(connectionFactory)) {
                ProducerConnectionContext pcc = ccf.createProducerConnectionContext(endpoint);
                MessageId mid =
                        new MessageProcessor().send(pcc, messageSent,
                                Collections.singletonMap(TEST_HEADER, TEST_VALUE));
                log.infof("Sent message [%s] with messageId [%s]", messageSent, mid);
            }

            BasicMessageWithExtraData<BasicMessage> envelopeReceived =
                    receiver.receivedMessages.poll(15, TimeUnit.SECONDS);
            Assert.assertNotNull("No message received", envelopeReceived);
            Assert.assertEquals(envelopeSent.getBasicMessage().toJSON(), envelopeReceived.getBasicMessage().toJSON());
            Assert.assertNull("envelopeReceived should have no binary attachment", envelopeReceived.getBinaryData());
        }

    }

    @Test
    public void textMessageWithBinary() throws JMSException, IOException, InterruptedException {
        assertNotNull(connectionFactory);

        Endpoint endpoint = new Endpoint(Type.QUEUE, TEST_QUEUE);

        try (MessageReceiver receiver = new MessageReceiver(connectionFactory, endpoint)) {
            byte[] bytesSent = "deadbeef".getBytes("utf-8");

            SimpleBasicMessage messageSent = new SimpleBasicMessage("textMessageWithBinary");
            BasicMessageWithExtraData<SimpleBasicMessage> envelopeSent =
                    new BasicMessageWithExtraData<SimpleBasicMessage>(messageSent,
                            new BinaryData(bytesSent, new ByteArrayInputStream(new byte[0])));
            try (ConnectionContextFactory ccf = new ConnectionContextFactory(connectionFactory)) {
                ProducerConnectionContext pcc = ccf.createProducerConnectionContext(endpoint);
                MessageId mid =
                        new MessageProcessor().send(pcc, envelopeSent,
                                Collections.singletonMap(TEST_HEADER, TEST_VALUE));
                log.infof("Sent message [%s] with messageId [%s]", messageSent, mid);
            }

            BasicMessageWithExtraData<BasicMessage> envelopeReceived =
                    receiver.receivedMessages.poll(15, TimeUnit.SECONDS);
            Assert.assertNotNull("No message received", envelopeReceived);
            Assert.assertEquals(envelopeSent.getBasicMessage().toJSON(), envelopeReceived.getBasicMessage().toJSON());

            ByteArrayOutputStream bytesReceived = new ByteArrayOutputStream(bytesSent.length);
            copy(envelopeReceived.getBinaryData(), bytesReceived);

            Assert.assertArrayEquals(bytesSent, bytesReceived.toByteArray());
        }
    }
}
