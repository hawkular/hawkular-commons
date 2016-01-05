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

import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;

import org.hawkular.bus.common.BasicMessage;
import org.hawkular.bus.common.MessageSerializer;
import org.jboss.logging.Logger;

/**
 * A message listener that expects to receive a JSON-encoded BasicMessage or one of its subclasses. Implementors need
 * not worry about the JSON decoding as it is handled for you. This class can also be used as a base class for MDBs.

 * @author John Mazzitelli
 */

public abstract class BasicMessageListener<T extends BasicMessage> implements MessageListener {

    private static final Logger log = Logger.getLogger(BasicMessageListener.class);

    @Inject
    private MessageSerializer messageSerializer;

    @Override
    public void onMessage(Message message) {

        log.debugf("Received raw message [%s]", message);

        T basicMessage = messageSerializer.toBasicMessage(message);
        onBasicMessage(basicMessage);
    }

    /**
     * This callback method is invoked after the raw JMS message is mapped to the message of type {@link T}.
     *
     * @param basicMessage The received message.
     *
     */
    public abstract void onBasicMessage(T basicMessage);
}
