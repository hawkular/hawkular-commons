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
package org.hawkular.bus.common.consumer;

import java.io.IOException;
import java.io.InputStream;

import javax.jms.BytesMessage;
import javax.jms.JMSException;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 */
public class BytesMessageInputStream extends InputStream {

    private final BytesMessage delegate;

    public BytesMessageInputStream(BytesMessage delegate) {
        super();
        this.delegate = delegate;
    }

    /** @see java.io.InputStream#read() */
    @Override
    public int read() throws IOException {
        try {
            return delegate.readByte();
        } catch (JMSException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        try {
            return delegate.readBytes(b);
        } catch (JMSException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (off == 0) {
            try {
                return delegate.readBytes(b, len);
            } catch (JMSException e) {
                throw new IOException(e);
            }
        } else {
            return super.read(b, off, len);
        }
    }

}
