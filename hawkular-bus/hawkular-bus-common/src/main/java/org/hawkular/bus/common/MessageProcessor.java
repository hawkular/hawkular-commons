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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;

import org.hawkular.bus.common.consumer.AbstractBasicMessageListener;
import org.hawkular.bus.common.consumer.BasicMessageListener;
import org.hawkular.bus.common.consumer.ConsumerConnectionContext;
import org.hawkular.bus.common.consumer.RPCConnectionContext;
import org.hawkular.bus.common.producer.ProducerConnectionContext;
import org.jboss.logging.Logger;

/**
 * Provides some functionality to process messages, both as a producer or consumer.
 *
 * Use {@link ConnectionContextFactory} to create contexts (which create destinations, sessions, and connections for
 * you) that you then use to pass to the listen and send methods in this class.
 */
public class MessageProcessor {

    private final Logger log = Logger.getLogger(MessageProcessor.class);

    public static final String HEADER_BASIC_MESSAGE_CLASS = "basicMessageClassName";

    /**
     * Listens for messages.
     *
     * @param context information that determines where to listen
     * @param listener the listener that processes the incoming messages
     * @throws JMSException any error
     *
     * @see org.hawkular.bus.common.ConnectionContextFactory#createConsumerConnectionContext(Endpoint)
     */
    public <T extends BasicMessage> void listen(ConsumerConnectionContext context,
            AbstractBasicMessageListener<T> listener) throws JMSException {
        if (context == null) {
            throw new NullPointerException("context must not be null");
        }
        if (listener == null) {
            throw new NullPointerException("listener must not be null");
        }

        MessageConsumer consumer = context.getMessageConsumer();
        if (consumer == null) {
            throw new NullPointerException("context had a null consumer");
        }

        listener.setConsumerConnectionContext(context);
        consumer.setMessageListener(listener);
    }

    /**
     * Same as {@link #send(ProducerConnectionContext, BasicMessage, Map)} with <code>null</code> headers.
     */
    public MessageId send(ProducerConnectionContext context, BasicMessage basicMessage) throws JMSException {
        return send(context, basicMessage, null);
    }

    /**
     * Send the given message to its destinations across the message bus. Once sent, the message will get assigned a
     * generated message ID. That message ID will also be returned by this method.
     *
     * Since this is fire-and-forget - no response is expected of the remote endpoint.
     *
     * @param context information that determines where the message is sent
     * @param basicMessage the message to send with optional headers included
     * @param headers headers for the JMS transport that will override same-named headers in the basic message
     * @return the message ID
     * @throws JMSException any error
     *
     * @see ConnectionContextFactory#createProducerConnectionContext(Endpoint)
     */
    public MessageId send(ProducerConnectionContext context, BasicMessage basicMessage, Map<String, String> headers)
            throws JMSException {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (basicMessage == null) {
            throw new IllegalArgumentException("message must not be null");
        }

        // create the JMS message to be sent
        Message msg = createMessage(context, basicMessage, headers);

        // if the message is correlated with another, put the correlation ID in the Message to be sent
        if (basicMessage.getCorrelationId() != null) {
            msg.setJMSCorrelationID(basicMessage.getCorrelationId().toString());
        }

        if (basicMessage.getMessageId() != null) {
            log.debugf("Non-null message ID [%s] will be ignored and a new one generated",
                    basicMessage.getMessageId());
            basicMessage.setMessageId(null);
        }

        // now send the message to the broker
        MessageProducer producer = context.getMessageProducer();
        if (producer == null) {
            throw new IllegalStateException("context had a null producer");
        }

        producer.send(msg);

        // put message ID into the message in case the caller wants to correlate it with another record
        MessageId messageId = new MessageId(msg.getJMSMessageID());
        basicMessage.setMessageId(messageId);

        return messageId;
    }

    /**
     * Same as {@link #sendWithBinaryData(ProducerConnectionContext, BasicMessage, InputStream, Map)} with
     * <code>null</code> headers.
     */
    public MessageId sendWithBinaryData(ProducerConnectionContext context, BasicMessage basicMessage,
            InputStream inputStream) throws JMSException {
        return sendWithBinaryData(context, basicMessage, inputStream, null);
    }

    /**
     * Same as {@link #sendWithBinaryData(ProducerConnectionContext, BasicMessage, File, Map)} with <code>null</code>
     * headers.
     *
     * @throws FileNotFoundException if the file does not exist
     */
    public MessageId sendWithBinaryData(ProducerConnectionContext context, BasicMessage basicMessage, File file)
            throws JMSException, FileNotFoundException {
        return sendWithBinaryData(context, basicMessage, new FileInputStream(file), null);
    }

    /**
     * Same as {@link #sendWithBinaryData(ProducerConnectionContext, BasicMessage, InputStream, Map)} with the input
     * stream being a stream to read the file.
     *
     * @throws FileNotFoundException if the file does not exist
     */
    public MessageId sendWithBinaryData(ProducerConnectionContext context, BasicMessage basicMessage, File file,
            Map<String, String> headers) throws JMSException, FileNotFoundException {
        return sendWithBinaryData(context, basicMessage, new FileInputStream(file), headers);
    }

    /**
     * If the given {@code message.getBinaryData()} is not {@code null} delegates to
     * {@link #sendWithBinaryData(ProducerConnectionContext, BasicMessage, InputStream, Map)} otherwise delegates to
     * {@link #send(ProducerConnectionContext, BasicMessageWithExtraData, Map)}
     *
     * @param context information that determines where the message is sent
     * @param message the message to send
     * @param headers headers for the JMS transport that will override same-named headers in the basic message
     * @return the message ID
     * @throws JMSException any error
     */
    public <T extends BasicMessage> MessageId send(ProducerConnectionContext context,
            BasicMessageWithExtraData<T> message, Map<String, String> headers) throws JMSException {
        if (message.getBinaryData() == null) {
            return send(context, message.getBasicMessage(), headers);
        } else {
            return sendWithBinaryData(context, message.getBasicMessage(), message.getBinaryData(), headers);
        }
    }

    /**
     * Send the given message along with the stream of binary data to its destinations across the message bus. Once
     * sent, the message will get assigned a generated message ID. That message ID will also be returned by this method.
     *
     * Since this is fire-and-forget - no response is expected of the remote endpoint.
     *
     * @param context information that determines where the message is sent
     * @param basicMessage the message to send with optional headers included
     * @param inputStream binary data that will be sent with the message
     * @param headers headers for the JMS transport that will override same-named headers in the basic message
     * @return the message ID
     * @throws JMSException any error
     *
     * @see ConnectionContextFactory#createProducerConnectionContext(Endpoint)
     */
    public MessageId sendWithBinaryData(ProducerConnectionContext context, BasicMessage basicMessage,
            InputStream inputStream, Map<String, String> headers) throws JMSException {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (basicMessage == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        if (inputStream == null) {
            throw new IllegalArgumentException("binary data must not be null");
        }

        // create the JMS message to be sent
        Message msg = createMessageWithBinaryData(context, basicMessage, inputStream, headers);

        // if the message is correlated with another, put the correlation ID in the Message to be sent
        if (basicMessage.getCorrelationId() != null) {
            msg.setJMSCorrelationID(basicMessage.getCorrelationId().toString());
        }

        if (basicMessage.getMessageId() != null) {
            log.debugf("Non-null message ID [%s] will be ignored and a new one generated",
                    basicMessage.getMessageId());
            basicMessage.setMessageId(null);
        }

        // now send the message to the broker
        MessageProducer producer = context.getMessageProducer();
        if (producer == null) {
            throw new IllegalStateException("context had a null producer");
        }

        producer.send(msg);

        // put message ID into the message in case the caller wants to correlate it with another record
        MessageId messageId = new MessageId(msg.getJMSMessageID());
        basicMessage.setMessageId(messageId);

        return messageId;
    }

    /**
     * Same as {@link #sendAndListen(ProducerConnectionContext, BasicMessage, BasicMessageListener, Map)} with
     * <code>null</code> headers.
     */
    public <T extends BasicMessage> RPCConnectionContext sendAndListen(ProducerConnectionContext context,
            BasicMessage basicMessage, BasicMessageListener<T> responseListener) throws JMSException {
        return sendAndListen(context, basicMessage, responseListener, null);
    }

    /**
     * Send the given message to its destinations across the message bus and any response sent back will be passed to
     * the given listener. Use this for request-response messages where you expect to get a non-void response back.
     *
     * The response listener should close its associated consumer since typically there is only a single response that
     * is expected. This is left to the listener to do in case there are special circumstances where the listener does
     * expect multiple response messages.
     *
     * If the caller merely wants to wait for a single response and obtain the response message to process it further,
     * consider using instead the method {@link #sendRPC} and use its returned Future to wait for the response, rather
     * than having to supply your own response listener.
     *
     * @param context information that determines where the message is sent
     * @param basicMessage the request message to send with optional headers included
     * @param responseListener The listener that will process the response of the request. This listener should close
     *            its associated consumer when appropriate.
     * @param headers headers for the JMS transport that will override same-named headers in the basic message
     *
     * @return the RPC context which includes information about the handling of the expected response
     * @throws JMSException any error
     *
     * @see org.hawkular.bus.common.ConnectionContextFactory#createProducerConnectionContext(Endpoint)
     */
    public <T extends BasicMessage> RPCConnectionContext sendAndListen(ProducerConnectionContext context,
            BasicMessage basicMessage, BasicMessageListener<T> responseListener, Map<String, String> headers)
                    throws JMSException {

        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (basicMessage == null) {
            throw new IllegalArgumentException("message must not be null");
        }
        if (responseListener == null) {
            throw new IllegalArgumentException("response listener must not be null");
        }

        // create the JMS message to be sent
        Message msg = createMessage(context, basicMessage, headers);

        // if the message is correlated with another, put the correlation ID in the Message to be sent
        if (basicMessage.getCorrelationId() != null) {
            msg.setJMSCorrelationID(basicMessage.getCorrelationId().toString());
        }

        if (basicMessage.getMessageId() != null) {
            log.debugf("Non-null message ID [%s] will be ignored and a new one generated",
                    basicMessage.getMessageId());
            basicMessage.setMessageId(null);
        }

        MessageProducer producer = context.getMessageProducer();
        if (producer == null) {
            throw new NullPointerException("Cannot send request-response message - the producer is null");
        }

        // prepare for the response prior to sending the request
        Session session = context.getSession();
        if (session == null) {
            throw new NullPointerException("Cannot send request-response message - the session is null");
        }
        TemporaryQueue responseQueue = session.createTemporaryQueue();
        MessageConsumer responseConsumer = session.createConsumer(responseQueue);

        RPCConnectionContext rpcContext = new RPCConnectionContext();
        rpcContext.copy(context);
        rpcContext.setDestination(responseQueue);
        rpcContext.setMessageConsumer(responseConsumer);
        rpcContext.setRequestMessage(msg);
        rpcContext.setResponseListener(responseListener);

        responseListener.setConsumerConnectionContext(rpcContext);
        responseConsumer.setMessageListener(responseListener);

        msg.setJMSReplyTo(responseQueue);

        // now send the message to the broker
        producer.send(msg);

        // put message ID into the message in case the caller wants to correlate it with another record
        MessageId messageId = new MessageId(msg.getJMSMessageID());
        basicMessage.setMessageId(messageId);

        return rpcContext;
    }

    /**
     * Same as {@link #createMessage(ConnectionContext, BasicMessage, Map)} with <code>null</code> headers.
     */
    protected Message createMessage(ConnectionContext context, BasicMessage basicMessage) throws JMSException {
        return createMessage(context, basicMessage, null);
    }

    /**
     * Creates a text message that can be send via a producer that contains the given BasicMessage's JSON encoded data.
     *
     * @param context the context whose session is used to create the message
     * @param basicMessage contains the data that will be JSON-encoded and encapsulated in the created message, with
     *            optional headers included
     * @param headers headers for the Message that will override same-named headers in the basic message
     * @return the message that can be produced
     * @throws JMSException any error
     * @throws NullPointerException if the context is null or the context's session is null
     */
    protected Message createMessage(ConnectionContext context, BasicMessage basicMessage, Map<String, String> headers)
            throws JMSException {
        if (context == null) {
            throw new IllegalArgumentException("The context is null");
        }
        if (basicMessage == null) {
            throw new IllegalArgumentException("The message is null");
        }

        Session session = context.getSession();
        if (session == null) {
            throw new IllegalArgumentException("The context had a null session");
        }
        TextMessage msg = session.createTextMessage(basicMessage.toJSON());

        setHeaders(basicMessage, headers, msg);

        log.infof("Created text message [%s] with text [%s]", msg, msg.getText());

        return msg;
    }

    /**
     * First sets the {@link MessageProcessor#HEADER_BASIC_MESSAGE_CLASS} string property of {@code destination} to
     * {@code basicMessage.getClass().getName()}, then copies all headers from {@code basicMessage.getHeaders()} to
     * {@code destination} using {@link Message#setStringProperty(String, String)} and then does the same thing with the
     * supplied {@code headers}.
     *
     * @param basicMessage the {@link BasicMessage} to copy headers from
     * @param headers the headers to copy to {@code destination}
     * @param destination the {@link Message} to copy the headers to
     * @throws JMSException
     */
    protected void setHeaders(BasicMessage basicMessage, Map<String, String> headers, Message destination)
            throws JMSException {
        log.infof("Setting [%s] = [%s] on a message of type [%s]", MessageProcessor.HEADER_BASIC_MESSAGE_CLASS,
                basicMessage.getClass().getName(), destination.getClass().getName());
        destination.setStringProperty(MessageProcessor.HEADER_BASIC_MESSAGE_CLASS, basicMessage.getClass().getName());

        // if the basicMessage has headers, use those first
        Map<String, String> basicMessageHeaders = basicMessage.getHeaders();
        if (basicMessageHeaders != null) {
            for (Map.Entry<String, String> entry : basicMessageHeaders.entrySet()) {
                destination.setStringProperty(entry.getKey(), entry.getValue());
            }
        }

        // If we were given headers separately, add those now.
        // Notice these will override same-named headers that were found in the basic message itself.
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                destination.setStringProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Same as {@link #createMessage(ConnectionContext, BasicMessage, Map)} with <code>null</code> headers.
     */
    protected Message createMessageWithBinaryData(ConnectionContext context, BasicMessage basicMessage,
            InputStream inputStream) throws JMSException {
        return createMessageWithBinaryData(context, basicMessage, inputStream, null);
    }

    /**
     * Creates a blob message that can be send via a producer that contains the given BasicMessage's JSON encoded data
     * along with binary data.
     *
     * @param context the context whose session is used to create the message
     * @param basicMessage contains the data that will be JSON-encoded and encapsulated in the created message, with
     *            optional headers included
     * @param inputStream binary data that will be sent with the message
     * @param headers headers for the Message that will override same-named headers in the basic message
     * @return the message that can be produced
     * @throws JMSException any error
     * @throws NullPointerException if the context is null or the context's session is null
     */
    protected Message createMessageWithBinaryData(ConnectionContext context, BasicMessage basicMessage,
            InputStream inputStream, Map<String, String> headers) throws JMSException {
        if (context == null) {
            throw new IllegalArgumentException("The context is null");
        }
        if (basicMessage == null) {
            throw new IllegalArgumentException("The message is null");
        }
        if (inputStream == null) {
            throw new IllegalArgumentException("The binary data is null");
        }

        Session session = context.getSession();
        if (session == null) {
            throw new IllegalArgumentException("The context had a null session");
        }

        // we are going to use BinaryData which allows us to prefix the binary data with the JSON message
        BinaryData messagePlusBinaryData = new BinaryData(basicMessage.toJSON().getBytes(), inputStream);

        BytesMessage msg = session.createBytesMessage();
        msg.setObjectProperty("JMS_AMQ_InputStream", messagePlusBinaryData);

        setHeaders(basicMessage, headers, msg);

        log.infof("Created binary message [%s]", msg);

        return msg;
    }

}
