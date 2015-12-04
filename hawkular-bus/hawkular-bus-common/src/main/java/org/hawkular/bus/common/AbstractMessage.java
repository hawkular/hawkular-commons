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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hawkular.bus.common.msg.features.FailOnUnknownProperties;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Basic information that is sent over the message bus.
 *
 * The {@link #getMessageId() message ID} is assigned by the messaging framework and so typically is not explicitly set.
 *
 * The {@link #getCorrelationId() correlation ID} is a message ID of another message that was sent previously. This is
 * usually left unset unless this message needs to be correlated with another. As an example, when a process is stopped,
 * you can correlate the "Stopped" event with the "Stopping" event so you can later determine how long it took for the
 * process to stop.
 *
 * The {@link #getHeaders() headers} are normally those out-of-band properties that are sent with the message.
 */
public abstract class AbstractMessage implements BasicMessage {
    // these are passed out-of-band of the message body - these attributes will therefore not be JSON encoded
    @JsonIgnore
    private MessageId _messageId;
    @JsonIgnore
    private MessageId _correlationId;
    @JsonIgnore
    private Map<String, String> _headers;

    /**
     * Convenience static method that converts a JSON string to a particular message object.
     *
     * @param json the JSON string
     * @param clazz the class whose instance is represented by the JSON string
     *
     * @return the message object that was represented by the JSON string
     */
    public static <T extends BasicMessage> T fromJSON(String json, Class<T> clazz) {
        try {
            Method buildObjectMapperForDeserializationMethod = findBuildObjectMapperForDeserializationMethod(clazz);

            final ObjectMapper mapper = (ObjectMapper) buildObjectMapperForDeserializationMethod.invoke(null);
            if (FailOnUnknownProperties.class.isAssignableFrom(clazz)) {
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            }

            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new IllegalStateException("JSON message cannot be converted to object of type [" + clazz + "]", e);
        }
    }

    /**
     * Convenience static method that reads a JSON string from the given stream and converts the JSON
     * string to a particular message object. The input stream will remain open so the caller can
     * stream any extra data that might appear after it.
     *
     * Because of the way the JSON parser works, some extra data might have been read from the stream
     * that wasn't part of the JSON message but was part of the extra data that came with it. Because of this,
     * the caller should no longer use the given stream but instead read the extra data via the returned
     * object (see {@link BasicMessageWithExtraData#getBinaryData()}) since it will handle this condition
     * properly.
     *
     * @param in input stream that has a JSON string at the head.
     * @param clazz the class whose instance is represented by the JSON string
     *
     * @return a POJO that contains a message object that was represented by the JSON string found
     *         in the stream. This returned POJO will also contain a {@link BinaryData} object that you
     *         can use to stream any additional data that came in the given input stream.
     */
    public static <T extends BasicMessage> BasicMessageWithExtraData<T> fromJSON(InputStream in, Class<T> clazz) {
        final T obj;
        final byte[] remainder;
        try (JsonParser parser = new JsonFactory().configure(Feature.AUTO_CLOSE_SOURCE, false).createParser(in)) {
            Method buildObjectMapperForDeserializationMethod = findBuildObjectMapperForDeserializationMethod(clazz);

            final ObjectMapper mapper = (ObjectMapper) buildObjectMapperForDeserializationMethod.invoke(null);
            if (FailOnUnknownProperties.class.isAssignableFrom(clazz)) {
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
            }

            obj = mapper.readValue(parser, clazz);
            final ByteArrayOutputStream remainderStream = new ByteArrayOutputStream();
            final int released = parser.releaseBuffered(remainderStream);
            remainder = (released > 0) ? remainderStream.toByteArray() : new byte[0];
        } catch (Exception e) {
            throw new IllegalArgumentException("Stream cannot be converted to JSON object of type [" + clazz + "]", e);
        }
        return new BasicMessageWithExtraData<T>(obj, new BinaryData(remainder, in));
    }

    private static Method findBuildObjectMapperForDeserializationMethod(Class<? extends BasicMessage> clazz) {
        try {
            Method m = clazz.getDeclaredMethod("buildObjectMapperForDeserialization");
            return m;
        } catch (NoSuchMethodException e) {
            // the given subclass doesn't have a method to build a mapper, maybe its superclass does.
            // eventually we'll get to the AbstractMessage class and we know it does have one.
            return findBuildObjectMapperForDeserializationMethod((Class<? extends BasicMessage>) clazz.getSuperclass());
        }
    }

    /**
     * This is static, so really there is no true overriding it in subclasses.
     * However, fromJSON will do the proper reflection in order to invoke
     * the proper method for the class that is being deserialized. So subclasses
     * that want to provide their own ObjectMapper will define their own
     * method that matches the signature of this method and it will be used.
     *
     * @return object mapper to be used for deserializing JSON.
     */
    protected static ObjectMapper buildObjectMapperForDeserialization() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * Converts this message to its JSON string representation.
     *
     * @return JSON encoded data that represents this message.
     */
    @Override
    public String toJSON() {
        final ObjectMapper mapper = buildObjectMapperForSerialization();
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Object cannot be parsed as JSON.", e);
        }
    }

    /**
     * @return object mapper to be used to serialize a message to JSON
     */
    protected ObjectMapper buildObjectMapperForSerialization() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        return mapper;
    }

    protected AbstractMessage() {
        // Intentionally left blank
    }

    /**
     * Returns the message ID that was assigned to this message by the messaging infrastructure. This could be null if
     * the message has not been sent yet.
     *
     * @return message ID assigned to this message by the messaging framework
     */
    @Override
    public MessageId getMessageId() {
        return _messageId;
    }

    @Override
    public void setMessageId(MessageId messageId) {
        this._messageId = messageId;
    }

    /**
     * If this message is correlated with another message, this will be that other message's ID. This could be null if
     * the message is not correlated with another message.
     *
     * @return the message ID of the correlated message
     */
    @Override
    public MessageId getCorrelationId() {
        return _correlationId;
    }

    @Override
    public void setCorrelationId(MessageId correlationId) {
        this._correlationId = correlationId;
    }

    /**
     * The headers that were shipped along side of the message when the message was received.
     * The returned map is an unmodifiable read-only view of the properties.
     * Will never return <code>null</code> but may return an empty map.
     *
     * @return a read-only view of the name/value properties that came with the message as separate headers.
     */
    @Override
    public Map<String, String> getHeaders() {
        if (_headers == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(_headers);
    }

    /**
     * Sets headers that will be sent with the message when the message gets delivered.
     * This completely replaces any existing headers already associated with this message.
     * Note that the given name/value pairs will be copied to an internal map.
     * If the given map is null or empty, this message's internal map will be destroyed
     * and {@link #getHeaders()} will return an empty map.
     *
     * Note that the header values are all expected to be strings.
     *
     * @param headers name/value properties
     */
    @Override
    public void setHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            this._headers = null;
        } else {
            if (this._headers == null) {
                this._headers = new HashMap<String, String>(headers);
            } else {
                // we want to replace what we had with the new headers
                this._headers.clear();
                this._headers.putAll(headers);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder(this.getClass().getSimpleName() + ": [");
        str.append("message-id=");
        str.append(getMessageId());
        str.append(", correlation-id=");
        str.append(getCorrelationId());
        str.append(", headers=");
        str.append(getHeaders());
        str.append(", json-body=[");
        str.append(toJSON());
        str.append("]]");
        return str.toString();
    }
}
