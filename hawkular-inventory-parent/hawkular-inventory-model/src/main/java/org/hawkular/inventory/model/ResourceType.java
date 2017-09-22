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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.hibernate.search.annotations.Indexed;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Joel Takvorian
 */
@Indexed
public class ResourceType implements Serializable {

    @JsonInclude(Include.NON_NULL)
    private final String id;  // Unique index [Search resource type by id]

    @JsonInclude(Include.NON_NULL)
    private final Collection<Operation> operations;

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
