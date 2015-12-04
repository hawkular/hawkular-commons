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

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A message that contains a complex object, which gets serialized into JSON.
 *
 * Use this class to send and receive ad-hoc objects - that is, ones that do not extend from {@link AbstractMessage}.
 *
 * @author Heiko W. Rupp
 * @author John Mazzitelli
 */
public class ObjectMessage extends AbstractMessage {
    @JsonInclude
    private String message; // the object in JSON form

    @JsonIgnore
    private Class<?> objectClass; // the ad-hoc class that this object message represents

    @JsonIgnore
    private final ObjectMapper mapper = new ObjectMapper();
    {
        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
            .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
            .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
            .withSetterVisibility(JsonAutoDetect.Visibility.NONE));
    }

    public ObjectMessage() {
    }

    public ObjectMessage(Object object) {
        if (object == null) {
            throw new IllegalArgumentException("object is null");
        }
        setObjectClass(object.getClass());

        final String msg;
        try {
            msg = mapper.writeValueAsString(object);
            setMessage(msg);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Object cannot be parsed as JSON.", e);
        }
    }

    public ObjectMessage(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz is null");
        }
        setObjectClass(clazz);
    }

    /**
     * The simple JSON representation of the object.
     *
     * @return message string as a JSON string
     */
    public String getMessage() {
        return message;
    }

    protected void setMessage(String msg) {
        this.message = msg;
    }

    public Class<?> getObjectClass() {
        return this.objectClass;
    }

    public void setObjectClass(Class<?> objectClass) {
        this.objectClass = objectClass;
    }

    public Object getObject() {
        Class<?> clazz = getObjectClass();
        if (clazz == null) {
            throw new IllegalStateException("Do not know what the class is that represents the JSON data");
        }

        try {
            return mapper.readValue(getMessage(), clazz);
        } catch (IOException e) {
            throw new IllegalStateException("JSON message cannot be converted to object.", e);
        }
    }
}
