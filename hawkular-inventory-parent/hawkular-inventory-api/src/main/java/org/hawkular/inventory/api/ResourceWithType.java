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
package org.hawkular.inventory.api;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.hawkular.inventory.model.Metric;
import org.hawkular.inventory.model.Resource;
import org.hawkular.inventory.model.ResourceType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Joel Takvorian
 */
public class ResourceWithType implements Serializable {

    @JsonInclude(Include.NON_NULL)
    private final String id;

    @JsonInclude(Include.NON_NULL)
    private final String name;

    @JsonInclude(Include.NON_NULL)
    private final Map<String, String> properties;

    @JsonInclude(Include.NON_NULL)
    private final ResourceType type;

    @JsonInclude(Include.NON_NULL)
    private final List<String> childrenIds;

    @JsonInclude(Include.NON_NULL)
    private final List<Metric> metrics;

    public ResourceWithType(@JsonProperty("id") String id,
                            @JsonProperty("name") String name,
                            @JsonProperty("properties") Map<String, String> properties,
                            @JsonProperty("type") ResourceType type,
                            @JsonProperty("childrenIds") List<String> childrenIds,
                            @JsonProperty("metrics") List<Metric> metrics) {
        this.id = id;
        this.name = name;
        this.properties = properties;
        this.type = type;
        this.childrenIds = childrenIds;
        this.metrics = metrics;
    }

    /**
     * Converts {@link Resource} into {@link ResourceWithType} using loader for {@link ResourceType}.
     * The children are not loaded.
     * @param r the resource to convert
     * @param rtLoader loader for {@link ResourceType}
     * @return the node without its subtree
     */
    public static ResourceWithType fromResource(Resource r,
                                                Function<String, ResourceType> rtLoader) {
        return new ResourceWithType(r.getId(), r.getName(), r.getProperties(), r.getType(rtLoader),
                r.getChildrenIds(), r.getMetrics());
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public ResourceType getType() {
        return type;
    }

    public List<String> getChildrenIds() {
        return childrenIds;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }
}
