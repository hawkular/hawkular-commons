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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hawkular.commons.doc.DocModel;
import org.hawkular.commons.doc.DocModelProperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Joel Takvorian
 */
@DocModel(description = "Representation of a resource type stored in the inventory. + \n")
public class ResourceType implements Serializable {

    public static class Builder {
        private String id;
        private List<Operation> operations = new ArrayList<>();
        private Map<String, String> properties = new HashMap<>();

        public ResourceType build() {
            return new ResourceType(id, operations, properties);
        }

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder operation(Operation op) {
            this.operations.add(op);
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

    @DocModelProperty(description = "Resource type identifier. Unique within the inventory.",
            position = 0,
            required = true)
    @JsonInclude(Include.NON_NULL)
    private final String id;  // Unique index [Search resource type by id]

    @DocModelProperty(description = "List of operations supported by this resource type.",
            position = 1,
            required = false)
    @JsonInclude(Include.NON_NULL)
    private final Collection<Operation> operations;

    @DocModelProperty(description = "Properties defined for this resource type.",
            position = 2)
    @JsonInclude(Include.NON_NULL)
    private final Map<String, String> properties;

    public ResourceType(@JsonProperty("id") String id,
                        @JsonProperty("operations") Collection<Operation> operations,
                        @JsonProperty("properties") Map<String, String> properties) {
        this.id = id;
        this.operations = operations;
        this.properties = properties;
    }

    public String getId() {
        return id;
    }

    public Collection<Operation> getOperations() {
        return Collections.unmodifiableCollection(operations);
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ResourceType that = (ResourceType) o;

        return id != null ? id.equals(that.id) : that.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ResourceType{" +
                "id='" + id + '\'' +
                ", operations=" + operations +
                ", properties=" + properties +
                '}';
    }
}
