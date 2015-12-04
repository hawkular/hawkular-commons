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

import java.io.InputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Enumeration;
import java.util.HashMap;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import org.hawkular.bus.common.AbstractMessage;
import org.hawkular.bus.common.BasicMessage;
import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.hawkular.bus.common.MessageId;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.log.MsgLogger;
import org.jboss.logging.Logger;

/**
 * A message listener that expects to receive a JSON-encoded BasicMessage or one of its subclasses; the JSON decoding is
 * handled for you.
 *
 * Subclasses will typically override {@link #AbstractBasicMessageListener(Class)} or
 * {@link #determineBasicMessageClass()} unless either (a) the subclass hierarchy has generic types that are specific
 * enough for reflection to determine the type of {@link BasicMessage} being listened for or (b) the message type being
 * listened for is {@link BasicMessage} and not one of its subclasses.
 */

public abstract class AbstractBasicMessageListener<T extends BasicMessage> implements MessageListener {

    private final MsgLogger msglog = MsgLogger.LOGGER;
    private final Logger log = Logger.getLogger(this.getClass());

    private ConsumerConnectionContext consumerConnectionContext;

    // In order to convert a JSON string to a BasicMessage object (or one of its subclasses), we need the actual Java
    // class of the generic type T. Java does not make it easy to find the class representation of T. This field will
    // store the actual class when we can actually determine what it is, which will be used when we decode a JSON string
    // into an instance of that class.
    private final Class<T> jsonDecoderRing;

    private final ClassLoader basicMessageClassLoader;

    public AbstractBasicMessageListener() {
        this.jsonDecoderRing = determineBasicMessageClass();
        this.basicMessageClassLoader = null;
    }

    /**
     * If a subclass knows the type and can give it to us, that will be the type used to decode JSON strings into that
     * message type. If this constructor is not used by subclasses, typically those subclasses will need to override
     * {@link #determineBasicMessageClass()} unless {@link BasicMessage} is the message type that subclass wants to
     * explicitly use (as opposed to a subclass of BasicMessage).
     *
     * @param jsonDecoderRing the class representation of the generic type T
     */
    protected AbstractBasicMessageListener(Class<T> jsonDecoderRing) {
        this.jsonDecoderRing = jsonDecoderRing;
        this.basicMessageClassLoader = null;
    }

    /**
     * A special constructor to be used when the desarialization should be based on the class name supplied in
     * {@link MessageProcessor#HEADER_BASIC_MESSAGE_CLASS} string property of {@link Message}. The given
     * {@link basicMessageClassLoader} should be able to resolve all types of messages the present listener can
     * encounter.
     *
     * @param basicMessageClassLoader the {@link ClassLoader} to resolve the class supplied in
     *            {@link MessageProcessor#HEADER_BASIC_MESSAGE_CLASS} string property of {@link Message}
     */
    protected AbstractBasicMessageListener(ClassLoader basicMessageClassLoader) {
        super();
        this.jsonDecoderRing = null;
        this.basicMessageClassLoader = basicMessageClassLoader;
    }

    /**
     * When this listener is attached to a consumer, this field should be filled in to allow the listener to perform
     * other tasks it needs which might require access to the context.
     *
     * @return the consumer context this listener is associated with, or <code>null</code> if not associated with a
     *         consumer yet
     */
    public ConsumerConnectionContext getConsumerConnectionContext() {
        return consumerConnectionContext;
    }

    public void setConsumerConnectionContext(ConsumerConnectionContext consumerConnectionContext) {
        this.consumerConnectionContext = consumerConnectionContext;
    }

    /**
     * Given the BasicMessage received over the wire, convert it to our T representation of the message and keep any
     * extra data that came with it.
     * <p>
     * The class T is determined as follows: First, the property {@link MessageProcessor#HEADER_BASIC_MESSAGE_CLASS} is
     * looked up in the given {@code message}'s properties if there is a class name set and if
     * {@link #basicMessageClassLoader} is not {@code null}, the class is gotten using
     * {@code Class.forName(className, true, basicMessageClassLoader)} (may throw an unchecked
     * {@link ClassNotFoundException}), otherwise {@link #getBasicMessageClass()} is used to get the Java type to
     * deserialize to.
     *
     * @param message the over-the-wire message
     *
     * @return the message as a object T, or null if we should not or cannot process the message
     */
    @SuppressWarnings("unchecked")
    protected BasicMessageWithExtraData<T> parseMessage(final Message message) {
        BasicMessageWithExtraData<T> retVal;
        try {
            Class<T> basicMessageClass = null;

            // If a basic message class name was provided to us in the header, we will try our best to use that
            // unless a subclass wants to substitute another class for it.
            String basicMessageClassName = message.getStringProperty(MessageProcessor.HEADER_BASIC_MESSAGE_CLASS);
            if (basicMessageClassName != null) {
                String desired = convertReceivedMessageClassNameToDesiredMessageClassName(basicMessageClassName);
                if (desired != null) {
                    basicMessageClassName = desired;
                }
                ClassLoader cl = (basicMessageClassLoader != null) ? basicMessageClassLoader
                        : this.getClass().getClassLoader();
                basicMessageClass = (Class<T>) Class.forName(basicMessageClassName, true, cl);
            } else {
                basicMessageClass = getBasicMessageClass();
            }
            log.debugf("Effective message type [%s]", basicMessageClass);

            if (message instanceof TextMessage) {
                String receivedBody = ((TextMessage) message).getText();
                T basicMessage = AbstractMessage.fromJSON(receivedBody, basicMessageClass);
                retVal = new BasicMessageWithExtraData<T>(basicMessage, null);

            } else if (message instanceof BytesMessage) {

                BytesMessage bytesMessage = (BytesMessage) message;
                InputStream receivedBody = new BytesMessageInputStream(bytesMessage);
                retVal = AbstractMessage.fromJSON(receivedBody, basicMessageClass);
            } else {
                throw new Exception("Unexpected implementation of " + Message.class.getName() + ": "
                        + message.getClass() + " expected " + TextMessage.class.getName() + " or "
                        + BytesMessage.class.getName() + ". Please report this bug.");
            }

            // grab some headers and put them in the message
            retVal.getBasicMessage().setMessageId(new MessageId(message.getJMSMessageID()));
            if (message.getJMSCorrelationID() != null) {
                MessageId correlationId = new MessageId(message.getJMSCorrelationID());
                retVal.getBasicMessage().setCorrelationId(correlationId);
            }

            HashMap<String, String> rawHeaders = new HashMap<String, String>();
            for (Enumeration<?> propNames = message.getPropertyNames(); propNames.hasMoreElements();) {
                String propName = propNames.nextElement().toString();
                rawHeaders.put(propName, message.getStringProperty(propName));
            }
            if (!rawHeaders.isEmpty()) {
                retVal.getBasicMessage().setHeaders(rawHeaders);
            }

            getLog().tracef("Received basic message: %s", retVal.getBasicMessage().getClass());

        } catch (JMSException e) {
            msglog.errorNotValidTextMessage(e);
            retVal = null;
        } catch (Exception e) {
            msglog.errorNotValidJsonMessage(e);
            retVal = null;
        }
        return retVal;
    }

    protected Class<T> getBasicMessageClass() {
        return jsonDecoderRing;
    }

    /**
     * In order to decode the JSON, we need the class representation of the basic message type. This method uses
     * reflection to try to get that type.
     *
     * Subclasses can override this if they want to provide the class representation themselves (e.g. in case the
     * reflection cannot get it). Alternatively, subclasses can utilize the constructor
     * {@link AbstractBasicMessageListener#AbstractBasicMessageListener(Class)} to tell this object what the class of T
     * is.
     *
     * @return class of T
     */
    @SuppressWarnings("unchecked")
    protected Class<T> determineBasicMessageClass() {
        // all of this is usually going to just return AbstractMessage.class - but in case there is a subclass hierarchy
        // that makes it more specific, this will help discover the message class.
        Class<?> thisClazz = this.getClass();
        Type superClazz = thisClazz.getGenericSuperclass();

        // we might be a internal generated class (like a MDB within a container) so walk up the hierarchy
        // to find our real paramaterized superclass
        while (superClazz instanceof Class) {
            superClazz = ((Class<?>) superClazz).getGenericSuperclass();
        }
        ParameterizedType parameterizedType = (ParameterizedType) superClazz;
        Type actualTypeArgument = parameterizedType.getActualTypeArguments()[0];
        Class<T> clazz;
        if (actualTypeArgument instanceof Class<?>) {
            clazz = (Class<T>) actualTypeArgument;
        } else {
            TypeVariable<?> typeVar = (TypeVariable<?>) actualTypeArgument;
            clazz = (Class<T>) typeVar.getBounds()[0];
        }
        return clazz;
    }

    /**
     * This allows subclasses to name a different JSON POJO implementation to use when deserializing
     * incoming JSON. When JSON messages are received with a classname specified as the one to deserialize
     * the JSON, this method lets you switch the classname so it will be used to deserialize the JSON.
     *
     * This is helpful if the JSON classname is not available on the classloader, but the listener instead
     * has another class that can be used to deserialize the JSON.
     *
     * This implementation always returns null. Subclasses are free to override.
     *
     * @param className the received JSON can be handled by this class
     * @return if not null, this will be the name of another class that is to be used to deserialize a JSON message
     */
    protected String convertReceivedMessageClassNameToDesiredMessageClassName(String className) {
        return null;
    }

    /**
     * @return logger for subclasses to use to log ad-hoc debug or trace messages
     */
    protected Logger getLog() {
        return this.log;
    }
}
