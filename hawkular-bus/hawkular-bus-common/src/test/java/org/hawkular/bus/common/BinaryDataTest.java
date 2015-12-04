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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("resource")
public class BinaryDataTest {

    @Test
    public void testBinaryWithNegativeOneValues() throws Exception {
        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        boas.write(-1);
        boas.write('a');
        boas.write('b');
        boas.write('c');
        boas.write(-1);
        boas.write(-1);
        boas.write(-1);
        boas.write('x');
        boas.write('y');
        boas.write('z');
        boas.write(-1);
        boas.close();

        final int totalDataSize = 11; // the number of bytes we wrote into our test string

        ByteArrayInputStream emptyIn = new ByteArrayInputStream(new byte[0]);
        BinaryData bd = new BinaryData(boas.toByteArray(), emptyIn);
        Assert.assertEquals(totalDataSize, bd.available());

        byte[] readArray = new byte[totalDataSize];
        Assert.assertEquals(totalDataSize, bd.read(readArray));
        int i = 0;
        Assert.assertEquals(-1, readArray[i++]);
        Assert.assertEquals('a', readArray[i++]);
        Assert.assertEquals('b', readArray[i++]);
        Assert.assertEquals('c', readArray[i++]);
        Assert.assertEquals(-1, readArray[i++]);
        Assert.assertEquals(-1, readArray[i++]);
        Assert.assertEquals(-1, readArray[i++]);
        Assert.assertEquals('x', readArray[i++]);
        Assert.assertEquals('y', readArray[i++]);
        Assert.assertEquals('z', readArray[i++]);
        Assert.assertEquals(-1, readArray[i++]);

        return;
    }

    @Test
    public void testEmptyBinaryData() throws Exception {
        BinaryData binaryData;

        binaryData = new BinaryData(null, buildInputStream(""));
        Assert.assertEquals(0, binaryData.available());
        Assert.assertEquals(-1, binaryData.read());
        binaryData = new BinaryData(buildByteArray(""), buildInputStream(""));
        Assert.assertEquals(0, binaryData.available());
        Assert.assertEquals(-1, binaryData.read());
    }

    @Test
    public void testClose() throws Exception {
        // because our tests use ByteArrayInputStream, close has no effect on it.
        // But close does have an effect on our in-memory data - closing the stream closes reads to the in-memory data
        BinaryData binaryData = new BinaryData(buildByteArray("123"), buildInputStream(""));

        // show we can start reading
        Assert.assertEquals(3, binaryData.available());
        Assert.assertEquals('1', binaryData.read());
        Assert.assertEquals(2, binaryData.available());

        // show that close worked
        binaryData.close();
        Assert.assertEquals(0, binaryData.available());
        Assert.assertEquals(-1, binaryData.read());
    }

    @Test
    public void testUnsupportedApis() {
        BinaryData binaryData = new BinaryData(buildByteArray(""), buildInputStream("123"));
        Assert.assertFalse(binaryData.markSupported());
        try {
            binaryData.reset();
            Assert.fail("Mark is not supported - an exception should have been thrown");
        } catch (IOException expected) {
        }
    }

    @Test
    public void testOnlyStreamData() throws Exception {
        actualTests("", "1234567890");
    }

    @Test
    public void testOnlyInMemoryData() throws Exception {
        actualTests("1234567890", "");
    }

    @Test
    public void testBothInMemoryDataAndStreamData() throws Exception {
        actualTests("12345", "67890");
    }

    // callers MUST assure that the concatenation of byteArray and inputStream will always be "1234567890"
    private void actualTests(String byteArrayString, String inputStreamString) throws Exception {
        BinaryData binaryData;
        byte[] bytes;

        // available()
        binaryData = new BinaryData(buildByteArray(byteArrayString), buildInputStream(inputStreamString));
        Assert.assertEquals(10, binaryData.available());
        Assert.assertEquals('1', binaryData.read());
        Assert.assertEquals(9, binaryData.available());
        Assert.assertEquals('2', binaryData.read());
        Assert.assertEquals(8, binaryData.available());
        Assert.assertEquals('3', binaryData.read());
        Assert.assertEquals(7, binaryData.available());
        Assert.assertEquals('4', binaryData.read());
        Assert.assertEquals(6, binaryData.available());
        Assert.assertEquals('5', binaryData.read());
        Assert.assertEquals(5, binaryData.available());
        Assert.assertEquals('6', binaryData.read());
        Assert.assertEquals(4, binaryData.available());
        Assert.assertEquals('7', binaryData.read());
        Assert.assertEquals(3, binaryData.available());
        Assert.assertEquals('8', binaryData.read());
        Assert.assertEquals(2, binaryData.available());
        Assert.assertEquals('9', binaryData.read());
        Assert.assertEquals(1, binaryData.available());
        Assert.assertEquals('0', binaryData.read());
        Assert.assertEquals(0, binaryData.available());
        Assert.assertEquals(-1, binaryData.read());

        // read(byte[])
        binaryData = new BinaryData(buildByteArray(byteArrayString), buildInputStream(inputStreamString));
        bytes = new byte[0]; // according to javadocs, an array of length of zero always returns 0 from read
        Assert.assertEquals(0, binaryData.read(bytes));
        Assert.assertEquals("", new String(bytes, "UTF-8"));

        binaryData = new BinaryData(buildByteArray(byteArrayString), buildInputStream(inputStreamString));
        bytes = new byte[5];
        Assert.assertEquals(5, binaryData.read(bytes));
        Assert.assertEquals("12345", new String(bytes, "UTF-8"));
        Assert.assertEquals(5, binaryData.read(bytes));
        Assert.assertEquals("67890", new String(bytes, "UTF-8"));
        Assert.assertEquals(-1, binaryData.read(bytes));

        binaryData = new BinaryData(buildByteArray(byteArrayString), buildInputStream(inputStreamString));
        bytes = new byte[10];
        Assert.assertEquals(10, binaryData.read(bytes));
        Assert.assertEquals("1234567890", new String(bytes, "UTF-8"));
        Assert.assertEquals(-1, binaryData.read(bytes));

        binaryData = new BinaryData(buildByteArray(byteArrayString), buildInputStream(inputStreamString));
        bytes = new byte[15];
        Assert.assertEquals(10, binaryData.read(bytes));
        Assert.assertEquals("1234567890\0\0\0\0\0", new String(bytes, "UTF-8"));
        Assert.assertEquals(-1, binaryData.read(bytes));

        // read(byte[], int, int)
        binaryData = new BinaryData(buildByteArray(byteArrayString), buildInputStream(inputStreamString));
        bytes = new byte[0]; // according to javadocs, an array of length of zero always returns 0 from read
        Assert.assertEquals(0, binaryData.read(bytes, 0, 0));
        Assert.assertEquals("", new String(bytes, "UTF-8"));

        binaryData = new BinaryData(buildByteArray(byteArrayString), buildInputStream(inputStreamString));
        bytes = new byte[10];
        Assert.assertEquals(3, binaryData.read(bytes, 0, 3));
        Assert.assertEquals(4, binaryData.read(bytes, 3, 4));
        Assert.assertEquals(3, binaryData.read(bytes, 7, 3));
        Assert.assertEquals("1234567890", new String(bytes, "UTF-8"));
        Assert.assertEquals(-1, binaryData.read(bytes));

        // skip(int)
        binaryData = new BinaryData(buildByteArray(byteArrayString), buildInputStream(inputStreamString));
        Assert.assertEquals(3, binaryData.skip(3));
        Assert.assertEquals('4', binaryData.read());
        Assert.assertEquals(4, binaryData.skip(4));
        Assert.assertEquals('9', binaryData.read());
        Assert.assertEquals('0', binaryData.read());
        Assert.assertEquals(0, binaryData.skip(111));
        Assert.assertEquals(-1, binaryData.read());
    }

    private byte[] buildByteArray(String str) {
        return Arrays.copyOf(str.getBytes(), str.length());
    }

    private InputStream buildInputStream(String str) {
        return new ByteArrayInputStream(str.getBytes());
    }
}
