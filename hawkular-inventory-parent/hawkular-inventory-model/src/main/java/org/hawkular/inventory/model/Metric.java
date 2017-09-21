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
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Joel Takvorian
 */
public class Metric implements Serializable {

    @JsonInclude(Include.NON_NULL)
    private final String id;    // Unique index ; Random UUID?

    @JsonInclude(Include.NON_NULL)
    private final String name;  // Prometheus short name? This field may not be necessary, name could just be a metadata put in properties

    @JsonInclude(Include.NON_NULL)
    private final String type;  // Ex: Deployment status, Server availability

    @JsonInclude(Include.NON_NULL)
    private final MetricUnit unit;

    @JsonInclude(Include.NON_NULL)
    private final Integer collectionInterval;   // Not sure if we can get it from prometheus

    @JsonInclude(Include.NON_NULL)
    private final Map<String, String> properties;   // properties may contain, for instance, the full prometheus metric name

    public Metric(@JsonProperty("id") String id,
                  @JsonProperty("name") String name,
                  @JsonProperty("type") String type,
                  @JsonProperty("unit") MetricUnit unit,
                  @JsonProperty("collectionInterval") Integer collectionInterval,
                  @JsonProperty("properties") Map<String, String> properties) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.unit = unit;
        this.collectionInterval = collectionInterval;
        this.properties = properties;
    }

    public String getId() {
        return id;
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

    public Integer getCollectionInterval() {
        return collectionInterval;
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Metric metric = (Metric) o;

        return id != null ? id.equals(metric.id) : metric.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Metric{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", unit=" + unit +
                ", collectionInterval=" + collectionInterval +
                ", properties=" + properties +
                '}';
    }
}
