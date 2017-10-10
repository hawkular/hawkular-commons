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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * High-level model for full {@link Resource} with subtree, associated with their {@link ResourceType}
 * @author Joel Takvorian
 */
public class ResourceNode implements Serializable {

    @JsonInclude(Include.NON_NULL)
    private final String id;

    @JsonInclude(Include.NON_NULL)
    private final String name;

    @JsonInclude(Include.NON_NULL)
    private final String feedId;

    @JsonInclude(Include.NON_NULL)
    private final Map<String, String> properties;

    @JsonInclude(Include.NON_NULL)
    private final ResourceType type;

    @JsonInclude(Include.NON_NULL)
    private final List<ResourceNode> children;

    @JsonInclude(Include.NON_NULL)
    private final List<Metric> metrics;

    public ResourceNode(@JsonProperty("id") String id,
                        @JsonProperty("name") String name,
                        @JsonProperty("feedId") String feedId,
                        @JsonProperty("properties") Map<String, String> properties,
                        @JsonProperty("type") ResourceType type,
                        @JsonProperty("children") List<ResourceNode> children,
                        @JsonProperty("metrics") List<Metric> metrics) {
        this.id = id;
        this.name = name;
        this.feedId = feedId;
        this.properties = properties;
        this.type = type;
        this.children = children;
        this.metrics = metrics;
    }

    /**
     * Converts {@link Resource} into {@link ResourceNode} using loaders for {@link ResourceType} and {@code children}.
     * @param r the resource to convert
     * @param rtLoader loader for {@link ResourceType}
     * @param rLoader loader for {@link Resource} children
     * @return the node with its subtree
     */
    public static ResourceNode fromResource(Resource r,
                                     Function<String, ResourceType> rtLoader,
                                     Function<String, List<Resource>> rLoader) {
        return fromResource(r, rtLoader, rLoader, new HashSet<>());
    }

    private static ResourceNode fromResource(Resource r,
                                             Function<String, ResourceType> rtLoader,
                                             Function<String, List<Resource>> rLoader,
                                             Set<String> loaded) {
        if (loaded.contains(r.getId())) {
            throw new IllegalStateException("Cycle detected in the tree with id " + r.getId()
                    + "; aborting operation. The inventory is invalid.");
        }
        loaded.add(r.getId());
        List<ResourceNode> children = r.getChildren(rLoader).stream()
                .map(child -> fromResource(child, rtLoader, rLoader, loaded))
                .collect(Collectors.toList());
        return new ResourceNode(r.getId(), r.getName(), r.getFeedId(), r.getProperties(), r.getType(rtLoader),
                children, r.getMetrics());
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

    public Map<String, String> getProperties() {
        return properties;
    }

    public ResourceType getType() {
        return type;
    }

    public List<ResourceNode> getChildren() {
        return children;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }
}
