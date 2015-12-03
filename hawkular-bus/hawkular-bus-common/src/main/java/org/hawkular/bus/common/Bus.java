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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TemporaryQueue;

import org.hawkular.bus.common.consumer.BasicMessageListener;
import org.jboss.logging.Logger;

/**
 * This is a CDI bean that provides an API for both sending and receiving messages as well as for registering
 * message listeners.
 *
 * @author jsanda
 */
@Dependent
public class Bus {

    private static final Logger log = Logger.getLogger(Bus.class);

    public static final String HEADER_BASIC_MESSAGE_CLASS = "basicMessageClassName";

    /**
     * HawkularConnectionFactory is a non-pooled connection factory. It is important that we use a non-pooled factory
     * because JMS message listeners cannot be used in a web or enterprise application with a pooled connection factory.
     * This is mandated by the JEE spec.
     */
    @Inject
    @JMSConnectionFactory("java:/HawkularConnectionFactory")
    private JMSContext context;

    /**
     * We inject a connection factory because we need to use non-managed contexts with listeners. We need a separate
     * context per listener and need to explicitly clean up resources when listeners are done with them, namely call
     * JMSContext.close() to free any resources created from the context. Calling JMSContext.close() on a managed
     * context is not permitted and results in an exception.
     */
    @Resource(lookup = "java:/HawkularConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Inject
    private MessageSerializer messageSerializer;

    private static class RegistrationData {
        MessageListener listener;
        JMSContext context;
        JMSConsumer consumer;

        public RegistrationData(MessageListener listener, JMSContext context, JMSConsumer consumer) {
            this.listener = listener;
            this.context = context;
            this.consumer = consumer;
        }
    }

    private Map<Registration, RegistrationData> registrations = new ConcurrentHashMap<>();

    @PreDestroy
    public void shutdown() {
        registrations.keySet().forEach(this::cancel);
    }

    /**
     * <p>
     * Sends a message to the specified destination. By default the message is sent as a
     * {@link javax.jms.TextMessage TextMessage}; however when the message's {@link T#getBinaryData() binaryData}
     * property is, the message is sent as a {@link javax.jms.BytesMessage BytesMessage}. The input stream will be
     * streamed via the underlying messaging subsystem.
     * </p>
     * <p>
     * If <code>message</code> contains any headers, then they will be stored stored as string properties in the JMS
     * message. And if the correlationId is set on <code>message</code>, then JMSCorrelationId will be set on the JMS
     * message.
     * </p>
     *
     * @param destination A JMS queue or topic
     * @param message The message to send
     * @param <T>
     * @return The {@link MessageId id} set by the underlying messaging provider/broker.
     * @throws JMSException
     */
    public <T extends BasicMessage> MessageId send(Destination destination, T message) throws JMSException {
        return send(destination, message, Collections.emptyMap());
    }

    /**
     * <p>
     * Sends a message to the specified destination. By default the message is sent as a
     * {@link javax.jms.TextMessage TextMessage}; however when the message's {@link T#getBinaryData() binaryData}
     * property is, the message is sent as a {@link javax.jms.BytesMessage BytesMessage}. The input stream will be
     * streamed via the underlying messaging subsystem.
     * </p>
     * <p>
     * If <code>message</code> contains any headers, then they will be stored stored as string properties in the JMS
     * message. The additional <code>headers</code> will override any headers supplied in <code>message</code>. And if
     * the correlationId is set on <code>message</code>, then JMSCorrelationId will be set on the JMS message.
     * </p>
     *
     * @param destination A JMS queue or topic
     * @param message The message to send
     * @param headers Additional headers to add to the message which override headers stored in the message
     * @param <T>
     * @return The {@link MessageId id} set by the underlying messaging provider/broker.
     * @throws JMSException
     */
    public <T extends BasicMessage> MessageId send(Destination destination, T message, Map<String, String> headers)
        throws JMSException {
        Message jmsMessage = prepareJMSMessage(message, headers);

        context.createProducer().send(destination, jmsMessage);

        message.setMessageId(new MessageId(jmsMessage.getJMSMessageID()));

        return message.getMessageId();
    }

    private <T extends BasicMessage> Message prepareJMSMessage(T message, Map<String, String> headers)
            throws JMSException {
        Message jmsMessage;
        String json = messageSerializer.toJson(message);
        if (message.getBinaryData() == null) {
            jmsMessage = context.createTextMessage(json);
        } else {
            jmsMessage = context.createBytesMessage();
            BinaryData binaryData = new BinaryData(json.getBytes(), message.getBinaryData());
            jmsMessage.setObjectProperty("JMS_AMQ_InputStream", binaryData);
        }

        setHeaders(jmsMessage, message, headers);
        if (message.getReplyTo() != null) {
            jmsMessage.setJMSReplyTo(message.getReplyTo());
        }
        if (message.getCorrelationId() != null) {
            jmsMessage.setJMSCorrelationID(message.getCorrelationId().getId());
        }
        return jmsMessage;
    }

    private <T extends BasicMessage> void setHeaders(Message message, T basicMessage, Map<String, String> headers)
            throws JMSException {
        message.setStringProperty(HEADER_BASIC_MESSAGE_CLASS, basicMessage.getClass().getName());
        basicMessage.getHeaders().entrySet().stream().forEach(entry -> {
            try {
                message.setStringProperty(entry.getKey(), entry.getValue());
            } catch (JMSException e) {
                throw new RuntimeException("Failed to set header {key: " + entry.getKey() + ", value: " +
                        entry.getValue() + "}", e);
            }
        });
        headers.entrySet().stream().forEach(entry -> {
            try {
                message.setStringProperty(entry.getKey(), entry.getValue());
            } catch (JMSException e) {
                throw new RuntimeException("Failed to set header {key: " + entry.getKey() + ", value: " +
                        entry.getValue() + "}", e);
            }
        });
        if (basicMessage.getReplyTo() != null) {
            message.setJMSReplyTo(basicMessage.getReplyTo());
        }
    }

    /**
     * Receive a message from the specified destination. This method blocks for at most one millisecond waiting for a
     * message to arrive.
     *
     * @param destination A JMS queue or topic
     * @param <T>
     * @return The message
     */
    public <T extends BasicMessage> T receive(Destination destination) {
        // We do not want to block indefinitely so minimum timeout of 1 ms is used. A message listener can be used to
        // block indefinitely if that is desired.
        return receive(destination, 1);
    }

    /**
     * Receive a message from the specified destination, blocking for the specified <code>timeout</code> until a
     * message arrives.
     *
     * @param destination A JMS queue or topic
     * @param timeout A timeout in milliseconds that specifies the time to wait for a message
     * @param <T>
     * @return The message
     */
    @SuppressWarnings("unchecked")
    public <T extends BasicMessage> T receive(Destination destination, long timeout) {
        Message message = context.createConsumer(destination).receive(timeout);
        return messageSerializer.toBasicMessage(message);
    }

    /**
     * Receive a message from the specified destination using a message selector. Only messages with properties matching
     * the message selector expression will be delivered. This method blocks for at most one millisecond.
     *
     * @param destination A JMS queue or topic
     * @param messageSelector only messages with properties matching the message selector expression are delivered.
     *                        A value of null or an empty string indicates that there is no message selector for the
     *                        underlying JMSConsumer.
     * @param <T>
     * @return The message
     */
    public <T extends BasicMessage> T receive(Destination destination, String messageSelector) {
        return receive(destination, messageSelector, 1);
    }

    /**
     * Receive a message from the specified destination using a message selector. Only messages with properties matching
     * the message selector expression will be delivered.
     *
     * @param destination A JMS queue or topic
     * @param messageSelector only messages with properties matching the message selector expression are delivered.
     *                        A value of null or an empty string indicates that there is no message selector for the
     *                        underlying JMSConsumer.
     * @param timeout Amount of time in milliseconds to block until a message arrives
     * @param <T>
     * @return The message
     */
    @SuppressWarnings("unchecked")
    public <T extends BasicMessage> T receive(Destination destination, String messageSelector, long timeout) {
        Message message = context.createConsumer(destination, messageSelector).receive(timeout);
        if (message == null) {
            return null;
        }
        return messageSerializer.toBasicMessage(message);
    }

    /**
     * <p>
     * Provides an RPC-style of message passing. The message is sent to the specified destination and the JMS message's
     * {@link Message#getJMSReplyTo() JMSReplyTo} property is set to a temporary queue. This method then waits up to
     * one millisecond (after the message has been sent) for a reply message to arrive in the temporary queue.
     * </p>
     * <p>
     * The temporary queue exists from the time of this method call for the remainder duration of the {@link Bus} life
     * time. Because the bus has {@link Dependent} scope, its lifetime is determined by the scope of the object into
     * which it is injected. This should be taken into consideration when using this method. If you inject the bus into
     * an object with {@link javax.enterprise.context.ApplicationScoped appliction scope} for example, the temporary
     * queues created in this method will exist for the remainder of the application.
     * </p>
     * @param destination A JMS queue or topic
     * @param request The request message
     * @param <Request>
     * @param <Response>
     * @return A response message
     * @throws JMSException
     */
    public <Request extends BasicMessage, Response extends BasicMessage> Response sendAndReceive(
            Destination destination, Request request) throws JMSException {
        return sendAndReceive(destination, request, 1);
    }

    /**
     * <p>
     * Provides an RPC-style of message passing. The message is sent to the specified destination and the JMS message's
     * {@link Message#getJMSReplyTo() JMSReplyTo} property is set to a temporary queue. This method then waits up to
     * one millisecond (after the message has been sent) for a reply message to arrive in the temporary queue.
     * </p>
     * <p>
     * The temporary queue exists from the time of this method call for the remainder duration of the {@link Bus} life
     * time. Because the bus has {@link Dependent} scope, its lifetime is determined by the scope of the object into
     * which it is injected. This should be taken into consideration when using this method. If you inject the bus into
     * an object with {@link javax.enterprise.context.ApplicationScoped appliction scope} for example, the temporary
     * queues created in this method will exist for the remainder of the application.
     * </p>
     * @param destination A JMS queue or topic
     * @param request The request message
     * @param <Request>
     * @param <Response>
     * @param timeout Amount of time in milliseconds to block until a response message arrives
     * @return A response message
     * @throws JMSException
     */
    @SuppressWarnings("unchecked")
    public <Request extends BasicMessage, Response extends BasicMessage> Response sendAndReceive(
            Destination destination, Request request, long timeout) throws JMSException {

        Message jmsRequest = prepareJMSMessage(request, Collections.emptyMap());
        TemporaryQueue responseQueue = context.createTemporaryQueue();
        jmsRequest.setJMSReplyTo(responseQueue);

        context.createProducer().send(destination, jmsRequest);

        Message jmsResponse = context.createConsumer(responseQueue).receive(timeout);

        if (jmsResponse == null) {
            return null;
        }
        return messageSerializer.toBasicMessage(jmsResponse);
    }

    /**
     * <p>
     * Register a listener to receive messages asynchronously from the specified destination. The listener's
     * {@link BasicMessageListener#onMessage(Message)} method will not be called, only the
     * {@link BasicMessageListener#onBasicMessage(BasicMessage)} method is called.
     * </p>
     * <p>
     * If the registration is not explicitly canceled, it will canceled when this bus instance is removed from the
     * container.
     * </p>
     *
     * @param destination A JMS queue or topic from which to listen for messages
     * @param listener The listener to which messages are to be delivered
     * @param <T>
     * @return A {@link Registration registration} that the bus uses to keep track of listeners. You can call its
     * {@link Registration#cancel() cancel} method to stop listening for messages.
     * @throws JMSException
     */
    @SuppressWarnings("unchecked")
    public <T extends BasicMessage> Registration register(Destination destination, BasicMessageListener<T> listener)
        throws JMSException {
        JMSContext listenerContext = connectionFactory.createContext();
        Registration registration = new Registration(this, destination, listener);
        JMSConsumer consumer = listenerContext.createConsumer(destination);
        MessageListener decorator = message -> {
            T basicMessage = messageSerializer.toBasicMessage(message);
            listener.onBasicMessage(basicMessage);
        };

        consumer.setMessageListener(decorator);
        registrations.put(registration, new RegistrationData(decorator, listenerContext, consumer));

        return registration;
    }

    /**
     * <p>
     * Register a listener to receive messages asynchronously from the specified destination. The listener's
     * {@link BasicMessageListener#onMessage(Message)} method will not be called, only the
     * {@link BasicMessageListener#onBasicMessage(BasicMessage)} method is called.
     * </p>
     * <p>
     * If the registration is not explicitly canceled, it will canceled when this bus instance is removed from the
     * container.
     * </p>
     *
     * @param destination A JMS queue or topic from which to listen for messages
     * @param listener The listener to which messages are to be delivered
     * @param messageSelector only messages with properties matching the message selector expression are delivered.
     *                        A value of null or an empty string indicates that there is no message selector for the
     *                        underlying JMSConsumer.
     * @param <T>
     * @return A {@link Registration registration} that the bus uses to keep track of listeners. You can call its
     * {@link Registration#cancel() cancel} method to stop listening for messages.
     * @throws JMSException
     */
    @SuppressWarnings("unchecked")
    public <T extends BasicMessage> Registration register(Destination destination, BasicMessageListener<T> listener,
        String messageSelector) throws JMSException {
        JMSContext listenerContext = connectionFactory.createContext();
        Registration registration = new Registration(this, destination, listener, messageSelector);
        JMSConsumer consumer = listenerContext.createConsumer(destination, messageSelector);
        MessageListener decorator = message -> {
            T basicMessage = messageSerializer.toBasicMessage(message);
            listener.onBasicMessage(basicMessage);
        };

        consumer.setMessageListener(decorator);
        registrations.put(registration, new RegistrationData(decorator, listenerContext, consumer));

        return registration;
    }

    void cancel(Registration registration) {
        RegistrationData data = registrations.remove(registration);
        if (data != null) {
            data.consumer.setMessageListener(null);
            data.consumer.close();
            data.context.close();
        }
    }

}
