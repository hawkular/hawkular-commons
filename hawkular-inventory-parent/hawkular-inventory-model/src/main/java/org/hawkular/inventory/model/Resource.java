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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Joel Takvorian
 */
@Indexed
public class Resource implements Serializable {

    public static class Builder {
        private String id;
        private String name;
        private String feedId;
        private String typeId;
        private boolean isRoot;
        private List<String> childrenIds = new ArrayList<>();
        private List<Metric> metrics = new ArrayList<>();
        private Map<String, String> properties = new HashMap<>();

        public Resource build() {
            return new Resource(id, name, feedId, typeId, isRoot, childrenIds, metrics, properties);
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder feedId(String feedId) {
            this.feedId = feedId;
            return this;
        }

        public Builder typeId(String typeId) {
            this.typeId = typeId;
            return this;
        }

        public Builder isRoot(boolean isRoot) {
            this.isRoot = isRoot;
            return this;
        }

        public Builder childId(String childId) {
            this.childrenIds.add(childId);
            return this;
        }

        public Builder metric(Metric metric) {
            this.metrics.add(metric);
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

    @JsonInclude(Include.NON_NULL)
    private final String id;

    @JsonInclude(Include.NON_NULL)
    private final String name;

    @JsonInclude(Include.NON_NULL)
    @Field(store = Store.YES, analyze = Analyze.NO, indexNullAs = Field.DEFAULT_NULL_TOKEN)
    private final String feedId;

    @JsonInclude(Include.NON_NULL)
    @Field(store = Store.YES, analyze = Analyze.NO, indexNullAs = Field.DEFAULT_NULL_TOKEN)
    private final String typeId;

    @JsonInclude(Include.NON_NULL)
    @Field(store = Store.YES, analyze = Analyze.NO, indexNullAs = Field.DEFAULT_NULL_TOKEN)
    private final boolean isRoot;

    @JsonInclude(Include.NON_NULL)
    private final List<String> childrenIds;

    @JsonInclude(Include.NON_NULL)
    private final List<Metric> metrics;

    @JsonInclude(Include.NON_NULL)
    private final Map<String, String> properties;

    // Lazy-loaded references
    private ResourceType type;
    private List<Resource> children;

    public Resource(@JsonProperty("id") String id,
                    @JsonProperty("name") String name,
                    @JsonProperty("feedId") String feedId,
                    @JsonProperty("typeId") String typeId,
                    @JsonProperty("root") boolean isRoot,
                    @JsonProperty("childrenIds") List<String> childrenIds,
                    @JsonProperty("metricIds") List<Metric> metrics,
                    @JsonProperty("properties") Map<String, String> properties) {
        this.id = id;
        this.name = name;
        this.feedId = feedId;
        this.typeId = typeId;
        this.isRoot = isRoot;
        this.childrenIds = childrenIds;
        this.metrics = metrics;
        this.properties = properties;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFeedId() {
        return feedId;
    }

    public String getTypeId() {
        return typeId;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public List<String> getChildrenIds() {
        return childrenIds;
    }

    public List<Metric> getMetrics() {
        return metrics;
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
                ", feedId='" + feedId + '\'' +
                ", typeId='" + typeId + '\'' +
                ", isRoot=" + isRoot +
                ", childrenIds=" + childrenIds +
                ", properties=" + properties +
                ", type=" + type +
                ", children=" + children +
                ", metrics=" + metrics +
                '}';
    }
}
