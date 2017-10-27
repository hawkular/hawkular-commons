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
package org.hawkular.inventory.api.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hawkular.commons.doc.DocModel;
import org.hawkular.commons.doc.DocModelProperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Joel Takvorian
 */
@DocModel(description = "Representation of a resource metric. + \n")
public class Metric implements Serializable {

    public static class Builder {
        private String name;
        private String type;
        private MetricUnit unit;
        private Map<String, String> properties = new HashMap<>();

        public Metric build() {
            return new Metric(name, type, unit, properties);
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder unit(MetricUnit unit) {
            this.unit = unit;
            return this;
        }

        public Builder property(String name, String value) {
            this.properties.put(name, value);
            return this;
        }

        public Builder properties(Map<String, String> props) {
            this.properties.putAll(props);
            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @DocModelProperty(description = "Metric name.",
            position = 0,
            required = true)
    @JsonInclude(Include.NON_NULL)
    private final String name;  // Name (for display?)

    @DocModelProperty(description = "Metric type.",
            position = 1,
            required = true)
    @JsonInclude(Include.NON_NULL)
    private final String type;  // Ex: Deployment status, Server availability

    @DocModelProperty(description = "Metric type.",
            position = 2,
            required = true,
            defaultValue = "NONE")
    @JsonInclude(Include.NON_NULL)
    private final MetricUnit unit;

    @DocModelProperty(description = "Metric properties.",
            position = 3,
            required = false)
    @JsonInclude(Include.NON_NULL)
    private final Map<String, String> properties;   // properties may contain, for instance, the full prometheus metric name

    public Metric(@JsonProperty("name") String name,
                  @JsonProperty("type") String type,
                  @JsonProperty("unit") MetricUnit unit,
                  @JsonProperty("properties") Map<String, String> properties) {
        this.name = name;
        this.type = type;
        this.unit = unit;
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public MetricUnit getUnit() {
        return unit;
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public String toString() {
        return "Metric{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", unit=" + unit +
                ", properties=" + properties +
                '}';
    }
}
