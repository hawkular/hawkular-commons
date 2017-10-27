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
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hawkular.commons.doc.DocModel;
import org.hawkular.commons.doc.DocModelProperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * High-level model for full {@link RawResource} with subtree, associated with their {@link ResourceType}
 * @author Joel Takvorian
 */
@DocModel(description = "Representation of a complete resource tree stored in the inventory. + \n" +
        "This resource embeds the <<ResourceType>> linked. + \n" +
        "This resource embeds recursively its children.")
public class ResourceNode implements Serializable {

    @DocModelProperty(description = "Resource identifier. Unique within the inventory.",
            position = 0,
            required = true)
    @JsonInclude(Include.NON_NULL)
    private final String id;

    @DocModelProperty(description = "Resource name. Used for display.",
            position = 1,
            required = true)
    @JsonInclude(Include.NON_NULL)
    private final String name;

    @DocModelProperty(description = "Feed identifier. Used to identify the agent that manages this resource.",
            position = 2,
            required = true)
    @JsonInclude(Include.NON_NULL)
    private final String feedId;

    @DocModelProperty(description = "<<ResourceType>> linked.",
            position = 3,
            required = true)
    @JsonInclude(Include.NON_NULL)
    private final ResourceType type;

    @DocModelProperty(description = "Parent resource identifier. Can be null if it's a root resource.",
            position = 4,
            required = true)
    @JsonInclude
    private final String parentId;

    @DocModelProperty(description = "A list of metrics defined for this resource.",
            position = 5)
    @JsonInclude(Include.NON_NULL)
    private final List<Metric> metrics;

    @DocModelProperty(description = "Properties defined for this resource.",
            position = 6)
    @JsonInclude(Include.NON_NULL)
    private final Map<String, String> properties;

    @DocModelProperty(description = "Configuration defined for this resource.",
            position = 7)
    @JsonInclude(Include.NON_NULL)
    private final Map<String, String> config;

    @DocModelProperty(description = "Resource tree children.",
            position = 8)
    @JsonInclude(Include.NON_NULL)
    private final List<ResourceNode> children;

    public ResourceNode(@JsonProperty("id") String id,
                        @JsonProperty("name") String name,
                        @JsonProperty("feedId") String feedId,
                        @JsonProperty("type") ResourceType type,
                        @JsonProperty("parentId") String parentId,
                        @JsonProperty("metrics") List<Metric> metrics,
                        @JsonProperty("properties") Map<String, String> properties,
                        @JsonProperty("config") Map<String, String> config,
                        @JsonProperty("children") List<ResourceNode> children) {
        this.id = id;
        this.name = name;
        this.feedId = feedId;
        this.type = type;
        this.parentId = parentId;
        this.metrics = metrics;
        this.properties = properties;
        this.config = config;
        this.children = children;
    }

    /**
     * Converts {@link RawResource} into {@link ResourceNode} using loaders for {@link ResourceType} and {@code children}.
     * @param r the resource to convert
     * @param rtLoader loader for {@link ResourceType}
     * @param rLoader loader for {@link RawResource} children
     * @return the node with its subtree
     */
    public static ResourceNode fromRaw(RawResource r,
                                       Function<String, Optional<ResourceType>> rtLoader,
                                       Function<String, List<RawResource>> rLoader) {
        return fromRaw(r, rtLoader, rLoader, new HashSet<>());
    }

    private static ResourceNode fromRaw(RawResource r,
                                        Function<String, Optional<ResourceType>> rtLoader,
                                        Function<String, List<RawResource>> rLoader,
                                        Set<String> loaded) {
        if (loaded.contains(r.getId())) {
            throw new IllegalStateException("Cycle detected in the tree with id " + r.getId()
                    + "; aborting operation. The inventory is invalid.");
        }
        loaded.add(r.getId());
        List<ResourceNode> children = rLoader.apply(r.getId()).stream()
                .map(child -> fromRaw(child, rtLoader, rLoader, loaded))
                .collect(Collectors.toList());
        return new ResourceNode(r.getId(), r.getName(), r.getFeedId(), rtLoader.apply(r.getTypeId()).orElse(null),
                r.getParentId(), r.getMetrics(), r.getProperties(), r.getConfig(), children);
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

    public String getParentId() {
        return parentId;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Map<String, String> getConfig() {
        return config;
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
