/*
 * Copyright 2014-2017 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.commons.json;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Json serialization/deserialization utility using Jackson implementation.
 *
 * @author Lucas Ponce
 * @author Joel Takvorian
 */
public final class JsonUtil {

    private static ObjectMapper MAPPER = new ObjectMapper();
    private static ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private JsonUtil() {
    }

    public static String toJson(Object resource) {
        try {
            return MAPPER.writeValueAsString(resource);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    public static <T> List<T> listFromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Object o) {
        return MAPPER.convertValue(o, Map.class);
    }

    public static ObjectMapper getMapper() {
        return MAPPER;
    }

    public static JsonGenerator createJsonGenerator(OutputStream os) throws IOException {
        JsonGenerator jsonGen = new JsonFactory().createGenerator(os, JsonEncoding.UTF8);
        jsonGen.setCodec(MAPPER);
        return jsonGen;
    }

    public static ObjectMapper getYamlMapper() {
        return YAML_MAPPER;
    }
}
