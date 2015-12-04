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

import javax.jms.Message;

import org.hawkular.bus.common.BasicMessage;
import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.jboss.logging.Logger;

/**
 * A message listener that expects to receive a JSON-encoded BasicMessage or one of its subclasses. Implementors need
 * not worry about the JSON decoding as it is handled for you
 *
 * Subclasses must override one and only one of the {@link #onBasicMessage(BasicMessageWithExtraData)} or
 * {@link #onBasicMessage(BasicMessage)} methods.
 *
 * This processes fire-and-forget requests - that is, the request message is processed with no response being sent back
 * to the sender.
 *
 * @author John Mazzitelli
 */

public abstract class BasicMessageListener<T extends BasicMessage> extends AbstractBasicMessageListener<T> {
    private static final Logger log = Logger.getLogger(BasicMessageListener.class);

    public BasicMessageListener() {
        super();
    }

    protected BasicMessageListener(Class<T> jsonDecoderRing) {
        super(jsonDecoderRing);
    }

    protected BasicMessageListener(ClassLoader basicMessageClassLoader) {
        super(basicMessageClassLoader);
    }

    @Override
    public void onMessage(Message message) {

        log.debugf("Received raw message [%s]", message);

        BasicMessageWithExtraData<T> msgWithExtraData = parseMessage(message);
        if (msgWithExtraData == null) {
            return; // either we are not to process this message or some error occurred, so we skip it
        }

        onBasicMessage(msgWithExtraData);
        return;
    };

    /**
     * Subclasses implement this method to process the received message.
     *
     * If subclasses would rather just receive the {@link BasicMessage}, it can do so by
     * overriding {@link #onBasicMessage(BasicMessage)} and leaving this method as-is (that is,
     * do NOT override this method).
     *
     * @param msgWithExtraData the basic message received with any extra data that came with it
     */
    protected void onBasicMessage(BasicMessageWithExtraData<T> msgWithExtraData) {
        onBasicMessage(msgWithExtraData.getBasicMessage()); // delegate
    }

    /**
     * Subclasses can implement this method rather than {@link #onBasicMessage(BasicMessageWithExtraData)}
     * if they only expect to receive a {@link BasicMessage} with no additional data.
     *
     * If this method is overridden by subclasses, then the {@link #onBasicMessage(BasicMessageWithExtraData)}
     * should not be.
     *
     * This base implementation is a no-op.
     *
     * @param basicMessage the basic message received
     */
    protected void onBasicMessage(T basicMessage) {
        // no op
    }
}
