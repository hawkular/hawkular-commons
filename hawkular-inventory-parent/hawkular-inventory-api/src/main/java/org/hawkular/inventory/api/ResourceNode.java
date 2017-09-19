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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hawkular.inventory.model.Metric;
import org.hawkular.inventory.model.Resource;
import org.hawkular.inventory.model.ResourceType;

/**
 * @author Joel Takvorian
 */
public class ResourceNode {
    private final String id;
    private final String name;
    private final Map<String, String> properties;
    private final ResourceType type;
    private final List<ResourceNode> children;
    private final List<Metric> metrics;

    public ResourceNode(String id, String name, Map<String, String> properties,
                        ResourceType type, List<ResourceNode> children,
                        List<Metric> metrics) {
        this.id = id;
        this.name = name;
        this.properties = properties;
        this.type = type;
        this.children = children;
        this.metrics = metrics;
    }

    public static ResourceNode fromResource(Resource r,
                                     Function<String, ResourceType> rtLoader,
                                     Function<String, Resource> rLoader,
                                     Function<String, Metric> mLoader) {
        return fromResource(r, rtLoader, rLoader, mLoader, new HashSet<>());
    }

    private static ResourceNode fromResource(Resource r,
                                             Function<String, ResourceType> rtLoader,
                                             Function<String, Resource> rLoader,
                                             Function<String, Metric> mLoader,
                                             Set<String> loaded) {
        if (loaded.contains(r.getId())) {
            throw new IllegalStateException("Cycle detected in the tree with id " + r.getId()
                    + "; aborting operation. The inventory is invalid.");
        }
        loaded.add(r.getId());
        List<ResourceNode> children = r.getChildren(rLoader).stream()
                .map(child -> fromResource(child, rtLoader, rLoader, mLoader, loaded))
                .collect(Collectors.toList());
        return new ResourceNode(r.getId(), r.getName(), r.getProperties(), r.getType(rtLoader),
                children, r.getMetrics(mLoader));
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

    public List<ResourceNode> getChildren() {
        return children;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }
}
