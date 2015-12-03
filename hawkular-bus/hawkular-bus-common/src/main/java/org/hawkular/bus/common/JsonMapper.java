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
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;

import org.hawkular.bus.common.msg.features.FailOnUnknownProperties;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author jsanda
 */
@Dependent
public class JsonMapper {

    private ObjectMapper defaultMapper;

    private ObjectMapper mapperFailUnknownProperties;

    private JsonFactory jsonFactory;

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

    public <T extends BasicMessage> T toBasicMessage(String json, Class<T> clazz) {
        try {
            return getMapper(clazz).readValue(json, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends BasicMessage> T toBasicMessage(InputStream inputStream, Class<T> clazz) {
        try {
            JsonParser parser = jsonFactory.createParser(inputStream);
            T basicMessage = getMapper(clazz).readValue(parser, clazz);
            ByteArrayOutputStream remainderStream = new ByteArrayOutputStream();
            int released = parser.releaseBuffered(remainderStream);
            byte[] remainder = (released > 0) ? remainderStream.toByteArray() : new byte[0];
            basicMessage.setBinaryData(new BinaryData(remainder, inputStream));

            return basicMessage;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public <T extends BasicMessage> String toJson(T message) {
        try {
            return defaultMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends BasicMessage> ObjectMapper getMapper(Class<T> clazz) {
        if (FailOnUnknownProperties.class.isAssignableFrom(clazz)) {
            return mapperFailUnknownProperties;
        }
        return defaultMapper;
    }

}
