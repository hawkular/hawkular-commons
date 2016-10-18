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
package org.hawkular.bus.common;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;

/**
 * This is a stream that is backed either by the provided InputStream or by a SequenceInputStream, should the
 * inMemoryData be provided.
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
    private InputStream backingStream;
    private Runnable onCloseAction;

    public BinaryData(byte[] inMemoryData, InputStream streamData) {
        if (null == inMemoryData || inMemoryData.length == 0) {
            backingStream = streamData;
        } else {
            ByteArrayInputStream firstStream = new ByteArrayInputStream(inMemoryData);
            backingStream = new SequenceInputStream(firstStream, streamData);
        }
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

    public void close() throws IOException {
        try {
            backingStream.close();
        } finally {
            // if we were asked to do something after close is done, do it now
            if (onCloseAction != null) {
                onCloseAction.run();
            }
        }
    }

    @Override
    public void mark(int readlimit) {
        backingStream.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        backingStream.reset();
    }

    @Override
    public boolean markSupported() {
        return backingStream.markSupported();
    }

    @Override
    public int read() throws IOException {
        return backingStream.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return backingStream.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return backingStream.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return backingStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return backingStream.available();
    }
}
