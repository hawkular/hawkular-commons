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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * High-level model for {@link RawResource} associated with {@link ResourceType}
 * Unlike {@link ResourceNode}, this class doesn't provide full children subtree,
 * but just a list of children ids instead.
 * @author Joel Takvorian
 */
public class Resource implements Serializable {

    @JsonInclude(Include.NON_NULL)
    private final String id;

    @JsonInclude(Include.NON_NULL)
    private final String name;

    @JsonInclude(Include.NON_NULL)
    private final String feedId;

    @JsonInclude(Include.NON_NULL)
    private final ResourceType type;

    @JsonInclude(Include.NON_NULL)
    private final List<Metric> metrics;

    @JsonInclude(Include.NON_NULL)
    private final Map<String, String> properties;

    @JsonInclude(Include.NON_NULL)
    private final Map<String, String> config;

    public Resource(@JsonProperty("id") String id,
                    @JsonProperty("name") String name,
                    @JsonProperty("feedId") String feedId,
                    @JsonProperty("type") ResourceType type,
                    @JsonProperty("metrics") List<Metric> metrics,
                    @JsonProperty("properties") Map<String, String> properties,
                    @JsonProperty("config") Map<String, String> config) {
        this.id = id;
        this.name = name;
        this.feedId = feedId;
        this.type = type;
        this.metrics = metrics;
        this.properties = properties;
        this.config = config;
    }

    /**
     * Converts {@link RawResource} into {@link Resource} using loader for {@link ResourceType}.
     * The children are not loaded.
     * @param r the resource to convert
     * @param rtLoader loader for {@link ResourceType}
     * @return the node without its subtree
     */
    public static Resource fromRaw(RawResource r, Function<String, Optional<ResourceType>> rtLoader) {
        return new Resource(r.getId(), r.getName(), r.getFeedId(), rtLoader.apply(r.getTypeId()).orElse(null),
                r.getMetrics(), r.getProperties(), r.getConfig());
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

    public Map<String, String> getConfig() {
        return config;
    }

    public ResourceType getType() {
        return type;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }
}
