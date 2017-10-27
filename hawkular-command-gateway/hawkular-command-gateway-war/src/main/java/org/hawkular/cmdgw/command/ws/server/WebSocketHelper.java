/*
 * Copyright 2014-2017 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.cmdgw.command.ws.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;

import javax.websocket.RemoteEndpoint.Async;
import javax.websocket.RemoteEndpoint.Basic;
import javax.websocket.Session;

import org.hawkular.bus.common.BasicMessage;
import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.hawkular.bus.common.BinaryData;
import org.hawkular.cmdgw.api.ApiDeserializer;
import org.hawkular.cmdgw.log.GatewayLoggers;
import org.hawkular.cmdgw.log.MsgLogger;

/**
 * Some convienence methods when working with WebSockets.
 */
public class WebSocketHelper {
    private static final MsgLogger log = GatewayLoggers.getLogger(WebSocketHelper.class);

    private Long asyncTimeout;

    public WebSocketHelper() {
        this.asyncTimeout = null;
    }

    /**
     * Creates a helper object.
     *
     * @param timeout number of milliseconds before an asynchronous send will timeout. A negative number means no
     *        timeout, null means use the WebSocket default timeout.
     */
    public WebSocketHelper(Long asyncTimeout) {
        this.asyncTimeout = asyncTimeout;
    }

    public void sendTextSync(Session session, String text) throws IOException {
        Basic basicRemote = session.getBasicRemote();
        basicRemote.sendText(text);
    }

    public void sendBasicMessageSync(Session session, BasicMessage msg) throws IOException {
        sendTextSync(session, ApiDeserializer.toHawkularFormat(msg));
    }

    public void sendTextAsync(Session session, String text) {
        Async asyncRemote = session.getAsyncRemote();
        if (this.asyncTimeout != null) {
            asyncRemote.setSendTimeout(this.asyncTimeout.longValue());
        }
        asyncRemote.sendText(text);
    }

    /**
     * Converts the given message to JSON and sends that JSON text to clients asynchronously.
     *
     * @param session the client session where the JSON message will be sent
     * @param msg the message to be converted to JSON and sent
     */
    public void sendBasicMessageAsync(Session session, BasicMessage msg) {
        String text = ApiDeserializer.toHawkularFormat(msg);
        sendTextAsync(session, text);
    }

    /**
     * Delegates to either {@link #sendBasicMessageSync(Session, BasicMessage)} or
     * {@link #sendBinarySync(Session, InputStream)} based on {@code message.getBinaryData() == null}.
     *
     * @param session the session to send to
     * @param message the message to send
     * @throws IOException
     */
    public void sendSync(Session session, BasicMessageWithExtraData<? extends BasicMessage> message)
            throws IOException {
        BinaryData binary = message.getBinaryData();
        if (binary == null) {
            sendBasicMessageSync(session, message.getBasicMessage());
        } else {
            // there is binary data to stream back - do it ourselves and don't return anything
            BinaryData serialized = ApiDeserializer.toHawkularFormat(message.getBasicMessage(),
                    message.getBinaryData());
            sendBinarySync(session, serialized);
        }
    }

    /**
     * Sends binary data to a client asynchronously.
     *
     * @param session the client session where the message will be sent
     * @param inputStream the binary data to send
     * @param threadPool where the job will be submitted so it can execute asynchronously
     */
    public void sendBinaryAsync(Session session, InputStream inputStream, ExecutorService threadPool) {
        if (session == null) {
            return;
        }

        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream must not be null");
        }

        log.debugf("Attempting to send async binary data to client [%s]", session.getId());

        if (session.isOpen()) {
            if (this.asyncTimeout != null) {
                // TODO: what to do with timeout?
            }

            CopyStreamRunnable runnable = new CopyStreamRunnable(session, inputStream);
            threadPool.execute(runnable);
        }

        return;
    }

    /**
     * Sends binary data to a client synchronously.
     *
     * @param session the client where the message will be sent
     * @param inputStream the binary data to send
     * @throws IOException if a problem occurred during delivery of the data to a session.
     */
    public void sendBinarySync(Session session, InputStream inputStream) throws IOException {
        if (session == null) {
            return;
        }

        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream must not be null");
        }

        log.debugf("Attempting to send binary data to client [%s]", session.getId());

        if (session.isOpen()) {
            long size = new CopyStreamRunnable(session, inputStream).copyInputToOutput();
            log.debugf("Finished sending binary data to client [%s]: size=[%s]", session.getId(), size);
        }

        return;
    }

    private class CopyStreamRunnable implements Runnable {
        private final Session session;
        private final InputStream inputStream;

        public CopyStreamRunnable(Session session, InputStream inputStream) {
            this.session = session;
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            try {
                long size = copyInputToOutput();
                log.debugf("Finished sending async binary data to client [%s]: size=[%s]", session.getId(), size);
            } catch (Exception e) {
                log.errorFailedSendingAsyncBinaryData(e, session.getId());
            }
        }

        public long copyInputToOutput() throws IOException {
            Basic basicRemote = session.getBasicRemote();
            OutputStream outputStream = basicRemote.getSendStream();

            try {
                // slurp the input stream data and send directly to the output stream
                byte[] buf = new byte[4096];
                long totalBytesCopied = 0L;
                while (true) {
                    int numRead = inputStream.read(buf);
                    if (numRead == -1) {
                        break;
                    }
                    outputStream.write(buf, 0, numRead);
                    totalBytesCopied += numRead;
                }
                return totalBytesCopied;
            } finally {
                try {
                    outputStream.close();
                } finally {
                    inputStream.close();
                }
            }
        }
    }
}
