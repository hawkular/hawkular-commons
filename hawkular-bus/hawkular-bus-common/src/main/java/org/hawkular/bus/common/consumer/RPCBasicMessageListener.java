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

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.Session;

import org.hawkular.bus.common.AbstractMessage;
import org.hawkular.bus.common.BasicMessage;
import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.log.MsgLogger;
import org.hawkular.bus.common.producer.ProducerConnectionContext;

/**
 * A listener that processes an incoming request that will require a response sent back to the sender of the request.
 *
 * Subclasses must override one and only one of the {@link #onBasicMessage(BasicMessageWithExtraData)} or
 * {@link #onBasicMessage(AbstractMessage)} methods.
 *
 * @author John Mazzitelli
 *
 * @param <T> the type of the incoming request message
 * @param <U> the type of the response message that is to be sent back to the request sender
 */
public abstract class RPCBasicMessageListener<T extends BasicMessage, U extends BasicMessage> extends
        AbstractBasicMessageListener<T> {

    private final MsgLogger msglog = MsgLogger.LOGGER;

    // this will be used to send our reply
    private MessageProcessor messageSender;

    /**
     * Initialize with a default message sender.
     */
    public RPCBasicMessageListener() {
        super();
        setMessageSender(new MessageProcessor());
    }

    public RPCBasicMessageListener(MessageProcessor messageSender) {
        super();
        setMessageSender(messageSender);
    }

    protected RPCBasicMessageListener(Class<T> jsonDecoderRing) {
        super(jsonDecoderRing);
        setMessageSender(new MessageProcessor());
    }

    protected RPCBasicMessageListener(Class<T> jsonDecoderRing, MessageProcessor messageSender) {
        super(jsonDecoderRing);
        setMessageSender(messageSender);
    }

    protected MessageProcessor getMessageSender() {
        return messageSender;
    }

    protected void setMessageSender(MessageProcessor messageSender) {
        this.messageSender = messageSender;
    }

    @Override
    public void onMessage(Message message) {
        BasicMessageWithExtraData<T> msgWithExtraData = parseMessage(message);
        if (msgWithExtraData == null) {
            return; // either we are not to process this message or some error occurred, so we skip it
        }

        U responseBasicMessage = onBasicMessage(msgWithExtraData);

        // send the response back to the sender of the request
        ConsumerConnectionContext consumerConnectionContext = null;
        try {
            Destination replyTo = message.getJMSReplyTo();

            if (replyTo != null) {
                getLog().debugf("RPC client asked to get response sent to [%s]", replyTo);

                MessageProcessor sender = getMessageSender();
                if (sender == null) {
                    msglog.errorNoMessageSenderInListener();
                    return;
                }

                consumerConnectionContext = getConsumerConnectionContext();
                if (consumerConnectionContext == null) {
                    msglog.errorNoConnectionContextInListener();
                    return;
                }

                // create a producer connection context so it uses the same connection information as our consumer, but
                // ensure that we send the response to where the client told us to send it.
                ProducerConnectionContext producerContext = new ProducerConnectionContext();
                producerContext.copy(consumerConnectionContext);
                producerContext.setDestination(replyTo);
                Session session = producerContext.getSession();
                if (session == null) {
                    msglog.errorNoSessionInListener();
                } else {
                    producerContext.setMessageProducer(session.createProducer(replyTo));
                    sender.send(producerContext, responseBasicMessage);
                }

            } else {
                getLog().debug("Sender did not tell us where to reply - will not send any response back");
            }
        } catch (Exception e) {
            msglog.errorFailedToSendResponse(e);
            return;
        } finally {
            if (consumerConnectionContext != null) {
                try {
                    consumerConnectionContext.close();
                } catch (IOException e) {
                    msglog.errorFailedToCloseResourcesToRPCClient(e);
                }
            }
        }
    }

    /**
     * Subclasses implement this method to process the received message.
     *
     * If subclasses would rather just receive the {@link AbstractMessage}, it can do so by
     * overriding the onBasicMessage method that just takes the message type as a parameter
     * and leaving this method as-is (that is, do NOT override this method).
     *
     * @param msgWithExtraData the basic message received with any extra data that came with it
     * @return the response message - this will be forwarded to the sender of the request message
     */
    protected U onBasicMessage(BasicMessageWithExtraData<T> msgWithExtraData) {
        return onBasicMessage(msgWithExtraData.getBasicMessage());
    }

    /**
     * Subclasses can implement this method rather than {@link #onBasicMessage(BasicMessageWithExtraData)}
     * if they only expect to receive a {@link AbstractMessage} with no additional data.
     *
     * If this method is overridden by subclasses, then the {@link #onBasicMessage(BasicMessageWithExtraData)}
     * should not be.
     *
     * This base implementation is a no-op.
     *
     * @param basicMessage the basic message received
     * @return the response message - this will be forwarded to the sender of the request message
     */
    protected U onBasicMessage(T basicMessage) {
        return null; // no op
    }
}
