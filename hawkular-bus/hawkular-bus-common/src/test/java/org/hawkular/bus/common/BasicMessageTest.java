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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class BasicMessageTest {

    @Test
    public void testHeaders() {
        // we don't get headers by default
        AbstractMessage msg = new SimpleBasicMessage("my msg");
        assertNotNull(msg.getHeaders());
        assertTrue(msg.getHeaders().isEmpty());

        // the headers are copied internally
        Map<String, String> headers = new HashMap<String, String>();
        msg.setHeaders(headers);
        assertNotNull(msg.getHeaders());
        assertTrue(msg.getHeaders().isEmpty());
        assertNotSame(headers, msg.getHeaders());

        // we can't change the headers via the map returned by the getter
        try {
            msg.getHeaders().put("not-allowed", "not-allowed");
            assert false : "Should not have been allowed to modify the returned map";
        } catch (UnsupportedOperationException expected) {
            // expected
        }

        // show that the setter completely replaces the old headers
        assertEquals(0, msg.getHeaders().size());
        headers.put("one", "111");
        headers.put("two", "222");
        msg.setHeaders(headers);
        assertEquals(2, msg.getHeaders().size());
        assertEquals("111", msg.getHeaders().get("one"));
        assertEquals("222", msg.getHeaders().get("two"));

        headers.clear();
        assertEquals("Clearing our map should not have affected the msg internal map", 2, msg.getHeaders().size());
        headers.put("foo", "bar");
        msg.setHeaders(headers);
        assertEquals(1, msg.getHeaders().size());
        assertEquals("bar", msg.getHeaders().get("foo"));
    }

    // tests a minimal basic record with no details
    @Test
    public void simpleConversion() {
        SimpleBasicMessage arec = new SimpleBasicMessage("my msg");
        String json = arec.toJSON();
        System.out.println(json);
        assertNotNull("missing JSON", json);

        SimpleBasicMessage arec2 = AbstractMessage.fromJSON(json, SimpleBasicMessage.class);
        assertNotNull("JSON conversion failed", arec2);
        assertNotSame(arec, arec2);
        assertEquals(arec.getMessage(), arec2.getMessage());
        assertEquals(arec.getDetails(), arec2.getDetails());
    }

    // test a full basic record with several details
    @Test
    public void fullConversion() {
        Map<String, String> details = new HashMap<String, String>();
        details.put("key1", "val1");
        details.put("secondkey", "secondval");

        SimpleBasicMessage arec = new SimpleBasicMessage("my msg", details);
        arec.setMessageId(new MessageId("12345"));
        arec.setCorrelationId(new MessageId("67890"));
        String json = arec.toJSON();
        System.out.println(json);
        assertNotNull("missing JSON", json);

        SimpleBasicMessage arec2 = SimpleBasicMessage.fromJSON(json, SimpleBasicMessage.class);
        assertNotNull("JSON conversion failed", arec2);
        assertNotSame(arec, arec2);
        assertNull("BasicMessage ID should not be encoded in JSON", arec2.getMessageId());
        assertNull("Correlation ID should not be encoded in JSON", arec2.getCorrelationId());
        assertEquals("my msg", arec2.getMessage());
        assertEquals(2, arec2.getDetails().size());
        assertEquals("val1", arec2.getDetails().get("key1"));
        assertEquals("secondval", arec2.getDetails().get("secondkey"));
        assertEquals(arec.getMessage(), arec2.getMessage());
        assertEquals(arec.getDetails(), arec2.getDetails());
    }

    @Test
    public void testUnmodifiableDetails() {
        Map<String, String> details = new HashMap<String, String>();
        details.put("key1", "val1");

        SimpleBasicMessage msg = new SimpleBasicMessage("a", details);

        try {
            msg.getDetails().put("key1", "CHANGE!");
            assert false : "Should not have been able to change the details map";
        } catch (UnsupportedOperationException expected) {
            // to be expected
        }

        // make sure it didn't change and its still the same
        assertEquals("val1", msg.getDetails().get("key1"));
    }

    @Test
    public void testReadFromInputStream() throws IOException {
        // tests that this can extract the JSON even if more data follows in the stream
        SimpleBasicMessage msg = new SimpleBasicMessage("test-msg", Collections.singletonMap("one", "111"));

        String json = msg.toJSON();
        String extra = "This is some extra data";
        String jsonPlusExtra = json + extra;

        ByteArrayInputStream in = new UncloseableByteArrayInputStream(jsonPlusExtra.getBytes());

        BasicMessageWithExtraData<SimpleBasicMessage> fromJson = AbstractMessage.fromJSON(in, SimpleBasicMessage.class);
        SimpleBasicMessage msg2 = fromJson.getBasicMessage();
        BinaryData leftoverFromJsonParser = fromJson.getBinaryData();

        Assert.assertEquals(msg.getMessage(), msg2.getMessage());
        Assert.assertEquals(msg.getDetails(), msg2.getDetails());

        // now make sure the stream still has our extra data that we can read now
        byte[] leftoverBytes = new byte[leftoverFromJsonParser.available()];
        leftoverFromJsonParser.read(leftoverBytes);

        String totalRemaining = new String(leftoverBytes, "UTF-8");
        Assert.assertEquals(extra.length(), totalRemaining.length());
        Assert.assertEquals(extra, totalRemaining);
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
