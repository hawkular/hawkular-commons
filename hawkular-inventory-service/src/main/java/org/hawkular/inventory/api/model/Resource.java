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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Joel Takvorian
 */
public class Resource {
    private final String id;  // Unique index [Search resource by id]
    private final String name;
    private final String typeId;  // Index [Search all resources of type xx]
    private final String feed;    // Index; But not sure if feeds are still in play if the inventory is built from directly prometheus scans
    private final String rootId;  // Nullable; Index [Search all resources under root xx]
    private final List<String> childrenIds;
    private final List<String> metricIds;
    private final Map<String, String> properties;

    // Lazy-loaded references
    private ResourceType type;
    private List<Resource> children;
    private List<Metric> metrics;

    public Resource(String id, String name, String typeId, String feed, String rootId, List<String> childrenIds,
                    List<String> metricIds, Map<String, String> properties) {
        this.id = id;
        this.name = name;
        this.typeId = typeId;
        this.feed = feed;
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

    public String getFeed() {
        return feed;
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
                    .collect(Collectors.toList());
        }
        return Collections.unmodifiableList(children);
    }

    public List<Metric> getMetrics(Function<String, Metric> loader) {
        // lazy loading
        if (metrics == null) {
            metrics = metricIds.stream()
                    .map(loader)
                    .collect(Collectors.toList());
        }
        return Collections.unmodifiableList(metrics);
    }
}
