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

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.hawkular.bus.common.consumer.BytesMessageInputStream;
import org.hawkular.bus.common.msg.features.FailOnUnknownProperties;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A utility class for mapping to/from messages and JSON.
 *
 * @author jsanda
 */
@Dependent
public class MessageSerializer {

    private ObjectMapper defaultMapper;

    private ObjectMapper mapperFailUnknownProperties;

    private JsonFactory jsonFactory;

    /**
     * This a a convenience method for creating and initializing MessageSerializer objects when running outside of a
     * CDI environments such as unit tests. This method should <strong>not</strong> be used when running in a CDI
     * environment.
     */
    public static MessageSerializer create() {
        MessageSerializer serializer = new MessageSerializer();
        serializer.init();

        return serializer;
    }

    @PostConstruct
    public void init() {
        defaultMapper = new ObjectMapper();
        defaultMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setVisibilityChecker(defaultMapper.getSerializationConfig().getDefaultVisibilityChecker()
                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                        .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                        .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                        .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));

        mapperFailUnknownProperties = defaultMapper.copy().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                true);
        jsonFactory = new JsonFactory().configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
    }

    /**
     * This allows clients to provide their own ObjectMapper to use when mapping to/from JSON.
     *
     * @param mapper The ObjectMapper to use mapping messages to/from JSON
     */
    public void setMapper(ObjectMapper mapper) {
        defaultMapper = mapper;
    }

    public <T extends BasicMessage> String toJson(T message) {
        try {
            return defaultMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends BasicMessage> T toBasicMessage(Message message) {
        try {
            String className = message.getStringProperty(Bus.HEADER_BASIC_MESSAGE_CLASS);
            Class<T> clazz = (Class<T>) Class.forName(className);
            ObjectMapper mapper = getMapper(clazz);
            T basicMessage;

            if (message instanceof TextMessage) {
                basicMessage = mapper.readValue(((TextMessage) message).getText(), clazz);
            } else if (message instanceof BytesMessage) {
                InputStream receivedBody = new BytesMessageInputStream((BytesMessage) message);
                JsonParser parser = jsonFactory.createParser(receivedBody);

                basicMessage = mapper.readValue(parser, clazz);

                ByteArrayOutputStream remainderStream = new ByteArrayOutputStream();
                int released = parser.releaseBuffered(remainderStream);
                byte[] remainder = (released > 0) ? remainderStream.toByteArray() : new byte[0];
                basicMessage.setBinaryData(new BinaryData(remainder, receivedBody));

            } else {
                throw new RuntimeException(message + " is not a supported message type");
            }
            basicMessage.setMessageId(new MessageId(message.getJMSMessageID()));
            basicMessage.setHeaders(getHeaders(message));
            if (message.getJMSReplyTo() != null) {
                basicMessage.setReplyTo(message.getJMSReplyTo());
            }
            if (message.getJMSCorrelationID() != null) {
                basicMessage.setCorrelationId(new MessageId(message.getJMSCorrelationID()));
            }
            return basicMessage;
        } catch (JMSException | ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String,String> getHeaders(Message message) throws JMSException {
        Function<String, String> getProperty = propertyName -> {
            try {
                return message.getStringProperty(propertyName);
            } catch (JMSException e) {
                throw new RuntimeException("Unable to get property [" + propertyName + "]", e);
            }
        };
        return (Map<String, String>) Collections.list(message.getPropertyNames()).stream()
                .filter(key -> !Bus.HEADER_BASIC_MESSAGE_CLASS.equals(key))
                .collect(toMap(identity(), getProperty));
    }

    private <T extends BasicMessage> ObjectMapper getMapper(Class<T> clazz) {
        if (FailOnUnknownProperties.class.isAssignableFrom(clazz)) {
            return mapperFailUnknownProperties;
        }
        return defaultMapper;
    }

}
