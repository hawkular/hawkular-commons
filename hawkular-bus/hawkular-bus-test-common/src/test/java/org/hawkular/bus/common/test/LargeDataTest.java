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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;

import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.hawkular.bus.common.BinaryData;
import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.Endpoint.Type;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.consumer.ConsumerConnectionContext;
import org.hawkular.bus.common.producer.ProducerConnectionContext;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests sending large data via streams.
 */
public class LargeDataTest {

    @Test
    public void testWithSimpleInputStream() throws Exception {
        ConnectionContextFactory consumerFactory = null;
        ConnectionContextFactory producerFactory = null;

        TCPEmbeddedBrokerWrapper broker = new TCPEmbeddedBrokerWrapper();
        broker.start();

        String storageLocation = getTmpDirectory();

        try {
            String brokerURL = broker.getBrokerURL();
            brokerURL += "?jms.blobTransferPolicy.uploadUrl=file:" + storageLocation;
            Endpoint endpoint = new Endpoint(Type.QUEUE, "testq");

            // mimic server-side
            consumerFactory = new ConnectionContextFactory(brokerURL);
            ConsumerConnectionContext consumerContext = consumerFactory.createConsumerConnectionContext(endpoint);
            MessageWithExtraDataTestListener<SpecificMessage> listener;
            listener = new MessageWithExtraDataTestListener<SpecificMessage>(SpecificMessage.class);
            MessageProcessor serverSideProcessor = new MessageProcessor();
            serverSideProcessor.listen(consumerContext, listener);

            // mimic client side
            producerFactory = new ConnectionContextFactory(brokerURL);
            ProducerConnectionContext producerContext = producerFactory.createProducerConnectionContext(endpoint);
            MessageProcessor clientSideProcessor = new MessageProcessor();

            // send with an input stream
            SpecificMessage specificMessage = new SpecificMessage("hello", null, "specific text");
            String outgoingExtraData = "1234567890";
            ByteArrayInputStream input = new ByteArrayInputStream(outgoingExtraData.getBytes());
            clientSideProcessor.sendWithBinaryData(producerContext, specificMessage, input);

            // wait for the message to flow and check that it and the streamed data arrived
            listener.waitForMessage(3);
            BasicMessageWithExtraData<SpecificMessage> receivedMsgWithData = listener
                    .getReceivedMessageWithExtraData();
            SpecificMessage receivedMsg = receivedMsgWithData.getBasicMessage();
            assertEquals("Should have received the message", receivedMsg.getSpecific(), "specific text");

            BinaryData binaryData = receivedMsgWithData.getBinaryData();
            byte[] incomingExtraData = new byte[outgoingExtraData.length()];
            Assert.assertEquals(outgoingExtraData.length(), binaryData.read(incomingExtraData));
            Assert.assertEquals(outgoingExtraData, new String(incomingExtraData, "UTF-8"));

            // make sure the data has been removed from the uploadUrl storage location
            binaryData.close(); // closing will force the backing file to be removed
            File[] blobsStillAround = new File(storageLocation).listFiles();
            Assert.assertEquals("Still have blobs: " + Arrays.asList(blobsStillAround), 0, blobsStillAround.length);

        } finally {
            // close everything
            producerFactory.close();
            consumerFactory.close();
            broker.stop();

            // clean the storage location
            File doomedDir = new File(storageLocation);
            File[] doomedFiles = doomedDir.listFiles();
            for (File doomedFile : (doomedFiles == null) ? new File[0] : doomedFiles) {
                doomedFile.delete();
            }
            doomedDir.delete();
        }

    }

    private String getTmpDirectory() {
        String tmpDir = System.getProperty("java.io.tmpdir");
        return tmpDir + File.separator + "HawkularLargeDataTest-" + System.currentTimeMillis();
    }
}
