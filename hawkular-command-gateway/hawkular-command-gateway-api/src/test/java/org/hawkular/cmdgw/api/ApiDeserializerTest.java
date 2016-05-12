/*
 * Copyright 2014-2016 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.cmdgw.api;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

import org.hawkular.bus.common.AbstractMessage;
import org.hawkular.bus.common.BasicMessage;
import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.hawkular.bus.common.BinaryData;
import org.junit.Assert;
import org.junit.Test;

public class ApiDeserializerTest {

    @Test
    public void testAuthMessage() {
        ApiDeserializer ad = new ApiDeserializer();

        String nameAndJson = EchoRequest.class.getName()
                + "={\"echoMessage\":\"msg\", \"authentication\":{\"username\":\"foo\", \"password\":\"bar\"}}";
        BasicMessage request = ad.deserialize(nameAndJson).getBasicMessage();
        Assert.assertTrue(request instanceof EchoRequest);
        EchoRequest echoRequest = (EchoRequest) request;
        Assert.assertEquals("msg", echoRequest.getEchoMessage());
        Assert.assertNotNull(echoRequest.getAuthentication());
        Assert.assertEquals("foo", echoRequest.getAuthentication().getUsername());
        Assert.assertEquals("bar", echoRequest.getAuthentication().getPassword());
        Assert.assertNull(echoRequest.getAuthentication().getToken());
        Assert.assertNull(echoRequest.getAuthentication().getPersona());

        nameAndJson = EchoRequest.class.getName()
                + "={\"echoMessage\":\"msg\", \"authentication\":{\"token\":\"tok\", \"persona\":\"polly\"}}";
        request = ad.deserialize(nameAndJson).getBasicMessage();
        Assert.assertTrue(request instanceof EchoRequest);
        echoRequest = (EchoRequest) request;
        Assert.assertEquals("msg", echoRequest.getEchoMessage());
        Assert.assertNotNull(echoRequest.getAuthentication());
        Assert.assertNull(echoRequest.getAuthentication().getUsername());
        Assert.assertNull(echoRequest.getAuthentication().getPassword());
        Assert.assertEquals("tok", echoRequest.getAuthentication().getToken());
        Assert.assertEquals("polly", echoRequest.getAuthentication().getPersona());
    }

    @Test
    public void testApiDeserializer() {
        ApiDeserializer ad = new ApiDeserializer();

        String nameAndJson = EchoRequest.class.getName() + "={\"echoMessage\":\"msg\"}";
        BasicMessage request = ad.deserialize(nameAndJson).getBasicMessage();
        Assert.assertTrue(request instanceof EchoRequest);
        EchoRequest echoRequest = (EchoRequest) request;
        Assert.assertEquals("msg", echoRequest.getEchoMessage());
    }

    @Test
    public void testApiDeserializerError() {
        ApiDeserializer ad = new ApiDeserializer();

        String nameAndJson = EchoRequest.class.getName() + "={\"boo\":\"msg\"}";
        try {
            ad.deserialize(nameAndJson);
            Assert.fail("Should not have deserialized, RuntimeException expected.");
        } catch (RuntimeException expected) {
            Throwable root = expected;
            while (root.getCause() != null) {
                root = root.getCause();
            }
            Assert.assertTrue("Expected an UnrecognizedPropertyException",
                    root.getClass().getSimpleName().equals("UnrecognizedPropertyException"));
            final String expectedPrefix = "Unrecognized field \"boo\"";
            Assert.assertTrue("[" + root.getMessage() + "] should start with [" + expectedPrefix + "]",
                    root.getMessage().startsWith(expectedPrefix));
        }
    }

    @Test
    public void testExecuteOperationRequest() {
        ExecuteOperationRequest newpojo;
        ExecuteOperationRequest pojo = new ExecuteOperationRequest();
        pojo.setOperationName("opname");
        pojo.setResourcePath("respath");
        pojo.setParameters(new HashMap<String, String>());
        pojo.getParameters().put("one", "1");
        pojo.getParameters().put("two", "22");
        pojo.getParameters().put("three", "333");

        newpojo = testSpecificPojo(pojo);
        Assert.assertEquals(pojo.getOperationName(), newpojo.getOperationName());
        Assert.assertEquals(pojo.getResourcePath(), newpojo.getResourcePath());
        Assert.assertEquals(pojo.getParameters(), newpojo.getParameters());
    }

    @Test
    public void testGenericSuccessResponse() {
        GenericSuccessResponse newpojo;
        GenericSuccessResponse pojo = new GenericSuccessResponse();
        pojo.setMessage("howdy!");

        newpojo = testSpecificPojo(pojo);
        Assert.assertEquals(pojo.getMessage(), newpojo.getMessage());
    }

    @Test
    public void testGenericErrorResponse() {
        GenericErrorResponse newpojo;
        GenericErrorResponse pojo = new GenericErrorResponse();
        pojo.setErrorMessage("howdy!");
        pojo.setStackTrace("stack trace here");

        newpojo = testSpecificPojo(pojo);
        Assert.assertEquals(pojo.getErrorMessage(), newpojo.getErrorMessage());
        Assert.assertEquals(pojo.getStackTrace(), newpojo.getStackTrace());
    }

    @Test
    public void testEchoRequest() {
        EchoRequest newpojo;
        EchoRequest pojo = new EchoRequest();
        pojo.setEchoMessage("howdy!");

        newpojo = testSpecificPojo(pojo);
        Assert.assertEquals(pojo.getEchoMessage(), newpojo.getEchoMessage());
    }

    @Test
    public void testEchoResponse() {
        EchoResponse newpojo;
        EchoResponse pojo = new EchoResponse();
        pojo.setReply("what up?");

        newpojo = testSpecificPojo(pojo);
        Assert.assertEquals(pojo.getReply(), newpojo.getReply());
    }

    @Test
    public void testMessageWithExtraData() {
        // serialize a message and some extra data
        final String testMessage = "this is the message";
        final String testExtraData = "this is extra data";

        GenericSuccessResponse msg = new GenericSuccessResponse();
        msg.setMessage(testMessage);
        ByteArrayInputStream extraData = new ByteArrayInputStream(testExtraData.getBytes());
        BinaryData fullData = ApiDeserializer.toHawkularFormat(msg, extraData);

        // now deserialize the data
        ApiDeserializer ad = new ApiDeserializer();
        BasicMessageWithExtraData<GenericSuccessResponse> deserializedFullData = ad.deserialize(fullData);
        GenericSuccessResponse deserializedMessage = deserializedFullData.getBasicMessage();
        BinaryData deserializedExtraData = deserializedFullData.getBinaryData();
        String deserializedExtraDataString = new Scanner(deserializedExtraData, "UTF-8").useDelimiter("\\A").next();

        // make sure the deserialized data matches what we serialized
        Assert.assertEquals(testMessage, deserializedMessage.getMessage());
        Assert.assertEquals(testExtraData, deserializedExtraDataString);

    }

    @Test
    public void testMessageWithLargeExtraData() throws Exception {
        // serialize a message and some extra data
        final String testMessage = "this is the message";
        final String testExtraData;

        // stream [0, 1, 2, ..., 255], repeat that 10 times
        byte[] bytes = new byte[0xff * 10];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i % 256);
        }
        testExtraData = new String(bytes, "UTF-8");

        GenericSuccessResponse msg = new GenericSuccessResponse();
        msg.setMessage(testMessage);
        ByteArrayInputStream extraData = new ByteArrayInputStream(testExtraData.getBytes());
        BinaryData fullData = ApiDeserializer.toHawkularFormat(msg, extraData);

        // now deserialize the data
        ApiDeserializer ad = new ApiDeserializer();
        BasicMessageWithExtraData<GenericSuccessResponse> deserializedFullData = ad.deserialize(fullData);
        GenericSuccessResponse deserializedMessage = deserializedFullData.getBasicMessage();
        BinaryData deserializedExtraData = deserializedFullData.getBinaryData();
        String deserializedExtraDataString = new Scanner(deserializedExtraData, "UTF-8").useDelimiter("\\A").next();

        // make sure the deserialized data matches what we serialized
        Assert.assertEquals(testMessage, deserializedMessage.getMessage());
        Assert.assertEquals(testExtraData, deserializedExtraDataString);

    }

    @Test
    public void testReadFromInputStreamWithExtraData() throws IOException {
        // tests that this can extract the JSON even if more data follows in the stream
        ApiDeserializer ad = new ApiDeserializer();

        String nameAndJson = EchoRequest.class.getName() + "={\"echoMessage\":\"msg\"}";
        String extra = "This is some extra data";
        String nameAndJsonPlusExtra = nameAndJson + extra;

        ByteArrayInputStream in = new UncloseableByteArrayInputStream(nameAndJsonPlusExtra.getBytes());

        BasicMessageWithExtraData<AbstractMessage> map = ad.deserialize(in);
        AbstractMessage request = map.getBasicMessage();
        Assert.assertTrue(request instanceof EchoRequest);
        EchoRequest echoRequest = (EchoRequest) request;
        Assert.assertEquals("msg", echoRequest.getEchoMessage());

        // now make sure the stream still has our extra data that we can read now
        BinaryData leftover = map.getBinaryData();
        byte[] leftoverByteArray = new byte[leftover.available()];
        leftover.read(leftoverByteArray);

        String totalRemaining = new String(leftoverByteArray, "UTF-8");
        Assert.assertEquals(extra.length(), totalRemaining.length());
        Assert.assertEquals(extra, totalRemaining);

        // as a quick test, show that an exception results if we give bogus data in the input stream
        in = new UncloseableByteArrayInputStream("this is not valid data".getBytes());
        try {
            ad.deserialize(in);
            Assert.fail("Should have thrown an exception - the stream had invalid data");
        } catch (Exception expected) {
        }
    }

    @Test
    public void testReadFromInputStreamWithNoExtraData() throws IOException {
        ApiDeserializer ad = new ApiDeserializer();

        String nameAndJson = EchoRequest.class.getName() + "={\"echoMessage\":\"msg\"}";
        ByteArrayInputStream in = new UncloseableByteArrayInputStream(nameAndJson.getBytes());

        BasicMessageWithExtraData<AbstractMessage> map = ad.deserialize(in);
        AbstractMessage request = map.getBasicMessage();
        Assert.assertTrue(request instanceof EchoRequest);
        EchoRequest echoRequest = (EchoRequest) request;
        Assert.assertEquals("msg", echoRequest.getEchoMessage());

        // now make sure the stream is empty
        BinaryData leftover = map.getBinaryData();
        Assert.assertEquals(0, leftover.available());
        Assert.assertEquals(0, in.available());
    }

    // takes a POJO, gets its JSON, then deserializes that JSON back into a POJO.
    private <T extends BasicMessage> T testSpecificPojo(T pojo) {
        String nameAndJson = String.format("%s=%s", pojo.getClass().getSimpleName(), pojo.toJSON());
        System.out.println("ApiDeserializerTest: " + nameAndJson);
        ApiDeserializer ad = new ApiDeserializer();
        T results = (T) ad.deserialize(nameAndJson).getBasicMessage();
        Assert.assertNotSame(pojo, results); // just sanity check
        return results;
    }

    // This is just to test that our JsonParser does NOT close the stream.
    // If close is called, that is bad and should fail the test
    class UncloseableByteArrayInputStream extends ByteArrayInputStream {
        public UncloseableByteArrayInputStream(byte[] buf) {
            super(buf);
        }

        @Override
        public void close() throws IOException {
            Assert.fail("The input stream should NOT have been closed");
        }
    }
}
