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

import static com.fasterxml.jackson.annotation.JsonInclude.*;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Joel Takvorian
 */
public class Resource implements Serializable {

    @JsonInclude(Include.NON_NULL)
    private final String id;  // Unique index [Search resource by id]

    @JsonInclude(Include.NON_NULL)
    private final String name;

    @JsonInclude(Include.NON_NULL)
    private final String typeId;  // Index [Search all resources of type xx]

    @JsonInclude(Include.NON_NULL)
    private final String rootId;  // Index [Search all resources under root xx]

    @JsonInclude(Include.NON_NULL)
    private final List<String> childrenIds;

    @JsonInclude(Include.NON_NULL)
    private final List<String> metricIds;

    @JsonInclude(Include.NON_NULL)
    private final Map<String, String> properties;

    // Lazy-loaded references
    private ResourceType type;
    private List<Resource> children;
    private List<Metric> metrics;

    public Resource(@JsonProperty("id") String id,
                    @JsonProperty("name") String name,
                    @JsonProperty("typeId") String typeId,
                    @JsonProperty("rootId") String rootId,
                    @JsonProperty("childrenIds") List<String> childrenIds,
                    @JsonProperty("metricIds") List<String> metricIds,
                    @JsonProperty("properties") Map<String, String> properties) {
        this.id = id;
        this.name = name;
        this.typeId = typeId;
        this.rootId = rootId;
        this.childrenIds = childrenIds;
        this.metricIds = metricIds;
        this.properties = properties;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTypeId() {
        return typeId;
    }

    public String getRootId() {
        return rootId;
    }

    public List<String> getChildrenIds() {
        return childrenIds;
    }

    public List<String> getMetricIds() {
        return metricIds;
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public ResourceType getType(Function<String, ResourceType> loader) {
        // lazy loading
        if (type == null) {
            type = loader.apply(typeId);
        }
        return type;
    }

    public List<Resource> getChildren(Function<String, Resource> loader) {
        // lazy loading
        if (children == null) {
            children = childrenIds.stream()
                    .map(loader)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return Collections.unmodifiableList(children);
    }

    public List<Metric> getMetrics(Function<String, Metric> loader) {
        // lazy loading
        if (metrics == null) {
            metrics = metricIds.stream()
                    .map(loader)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return Collections.unmodifiableList(metrics);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Resource resource = (Resource) o;

        return id != null ? id.equals(resource.id) : resource.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", typeId='" + typeId + '\'' +
                ", rootId='" + rootId + '\'' +
                ", childrenIds=" + childrenIds +
                ", metricIds=" + metricIds +
                ", properties=" + properties +
                ", type=" + type +
                ", children=" + children +
                ", metrics=" + metrics +
                '}';
    }
}
