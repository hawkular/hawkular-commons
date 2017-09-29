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
package org.hawkular.inventory.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Joel Takvorian
 */
public class Operation implements Serializable {

    public static class Builder {
        private String name;
        private Map<String, Map<String, String>> parameters = new HashMap<>();

        public Operation build() {
            return new Operation(name, parameters);
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder parameter(String name, Map<String, String> metadata) {
            this.parameters.put(name, metadata);
            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonInclude(Include.NON_NULL)
    private final String name;    // Ex: "Shutdown"

    @JsonInclude(Include.NON_NULL)
    private final Map<String, Map<String, String>> parameters;  // Ex: "restart" => {"type": "bool", "description": "If true, blablabla", "required": false}

    public Operation(@JsonProperty("name") String name,
                     @JsonProperty("parameters") Map<String, Map<String, String>> parameters) {
        this.name = name;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public Map<String, Map<String, String>> getParameters() {
        return parameters != null ? Collections.unmodifiableMap(parameters) : Collections.EMPTY_MAP;
    }
}
