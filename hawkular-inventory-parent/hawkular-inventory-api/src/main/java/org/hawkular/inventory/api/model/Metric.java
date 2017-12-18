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
        private String displayName;
        private String family;
        private MetricUnit unit;
        private String expression;
        private Map<String, String> labels = new HashMap<>();
        private Map<String, String> properties = new HashMap<>();

        public Metric build() {
            return new Metric(displayName, family, unit, expression, labels, properties);
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder family(String family) {
            this.family = family;
            return this;
        }

        public Builder unit(MetricUnit unit) {
            this.unit = unit;
            return this;
        }

        public Builder expression(String expression) {
            this.expression = expression;
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

        public Builder label(String name, String value) {
            this.labels.put(name, value);
            return this;
        }

        public Builder labels(Map<String, String> labels) {
            this.labels.putAll(labels);
            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @DocModelProperty(description = "Metric name for display.",
            position = 0,
            required = true)
    @JsonInclude(Include.NON_NULL)
    private final String displayName;

    @DocModelProperty(description = "Metric family name.",
            position = 1,
            required = true)
    @JsonInclude(Include.NON_NULL)
    private final String family;  // Ex: Prometheus family name

    @DocModelProperty(description = "Metric unit.",
            position = 2,
            required = true,
            defaultValue = "NONE")
    @JsonInclude(Include.NON_NULL)
    private final MetricUnit unit;

    @DocModelProperty(description = "Metric expression used to evaluate the metric value.",
            position = 3,
            required = true,
            defaultValue = "NONE")
    @JsonInclude(Include.NON_NULL)
    private final String expression;

    @DocModelProperty(description = "Metric labels.",
            position = 4,
            required = false)
    @JsonInclude(Include.NON_NULL)
    private final Map<String, String> labels;   // Ex: Prometheus labels

    @DocModelProperty(description = "Metric properties.",
            position = 5,
            required = false)
    @JsonInclude(Include.NON_NULL)
    private final Map<String, String> properties;

    public Metric(@JsonProperty("displayName") String displayName,
                  @JsonProperty("family") String family,
                  @JsonProperty("unit") MetricUnit unit,
                  @JsonProperty("expression") String expression,
                  @JsonProperty("labels") Map<String, String> labels,
                  @JsonProperty("properties") Map<String, String> properties) {
        this.displayName = displayName;
        this.family = family;
        this.unit = unit;
        this.expression = expression;
        this.labels = labels;
        this.properties = properties;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFamily() {
        return family;
    }

    public MetricUnit getUnit() {
        return unit;
    }

    public String getExpression() {
        return expression;
    }

    public Map<String, String> getLabels() {
        return Collections.unmodifiableMap(labels);
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public String toString() {
        return "Metric{" +
                "displayName='" + displayName + '\'' +
                ", family='" + family + '\'' +
                ", unit=" + unit +
                ", expression='" + expression + '\'' +
                ", labels=" + labels +
                ", properties=" + properties +
                '}';
    }
}
