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

import java.io.IOException;
import java.io.InputStream;

/**
 * This is a "stream" that includes some in-memory data as well as another stream.  The in-memory data (if not null
 * or empty) will be read first. Once the in-memory data has been exhausted, any additional reads will read data
 * from the input stream.
 *
 * This is used in the use-case that an input stream contained a JSON command (e.g. AbstractMessage) followed by
 * additional binary data. The JSON parser may have read extra data over and beyond the actual JSON message data.
 * In that case, the extra data the JSON parser read will be found in an in-memory byte array. That additional
 * in-memory data byte array combined with the input stream that may contain even more data will both be stored
 * in this object so both pieces of data can be accessed using the normal input stream API.
 *
 * This can also be used to prefix a binary blob (found in an input stream) with a byte array of in-memory data.
 * In other words, this can be used to prepare a message that consists of a JSON-message followed by a large
 * amount of data found in a stream.
 */
public class BinaryData extends InputStream {

    private byte[] inMemoryData;
    private final InputStream streamData;
    private int inMemoryDataPointer;

    private Runnable onCloseAction;

    public BinaryData(byte[] inMemoryData, InputStream streamData) {
        this.inMemoryData = (inMemoryData != null) ? inMemoryData : new byte[0];
        this.streamData = streamData;
        this.inMemoryDataPointer = 0;
        this.onCloseAction = null;
    }

    /**
     * Provides custom action to run after {@link #close()} finishes closing the stream.
     * This allows you to tell this object how it can clean up resources backing the stream.
     *
     * @param action the action or null if nothing should be done
     */
    public void setOnCloseAction(Runnable action) {
        onCloseAction = action;
    }

    public int read() throws IOException {
        if (unreadInMemoryDataExists()) {
            return (inMemoryData[inMemoryDataPointer++] & 0xff);
        } else {
            return streamData.read();
        }
    }

    public int read(byte[] b) throws IOException {
        if (unreadInMemoryDataExists()) {
            return super.read(b); // the superclass implementation is all we need
        } else {
            return this.streamData.read(b); // delegate directly to the stream
        }
    }

    public int read(byte[] b, int off, int len) throws IOException {
        if (unreadInMemoryDataExists()) {
            return super.read(b, off, len); // the superclass implementation is all we need
        } else {
            return this.streamData.read(b, off, len); // delegate directly to the stream
        }
    }

    public long skip(long n) throws IOException {
        if (unreadInMemoryDataExists()) {
            return super.skip(n); // the superclass implementation is all we need
        } else {
            return this.streamData.skip(n); // delegate directly to the stream
        }
    }

    public int available() throws IOException {
        return (inMemoryData.length - inMemoryDataPointer) + streamData.available();
    }

    public void close() throws IOException {
        // force nothing more to be read from the in-memory data, let the garbage collected free up its memory,
        // but avoid NPEs by setting inMemoryData to a small zero-byte array.
        inMemoryData = new byte[0];
        inMemoryDataPointer = 0;
        streamData.close();

        // if we were asked to do something after close is done, do it now
        if (onCloseAction != null) {
            onCloseAction.run();
        }
    }

    public void mark(int readlimit) {
        super.mark(readlimit); // superclass doesn't support this, and neither to we
    }

    public void reset() throws IOException {
        super.reset(); // superclass doesn't support this, and neither do we
    }

    public boolean markSupported() {
        return super.markSupported(); // superclass doesn't support this, and neither to we
    }

    private boolean unreadInMemoryDataExists() {
        return (inMemoryDataPointer < inMemoryData.length);
    }
}
