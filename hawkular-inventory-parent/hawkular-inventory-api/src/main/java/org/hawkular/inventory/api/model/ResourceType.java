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

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Joel Takvorian
 */
public class ResourceType {
    private final String id;  // Unique index [Search resource type by id]
    private final String feed;    // Index; But not sure if feeds are still in play if the inventory is built from directly prometheus scans
    private final Collection<Operation> operations;
    private final Map<String, String> properties;

    public ResourceType(String id, String feed, Collection<Operation> operations, Map<String, String> properties) {
        this.id = id;
        this.feed = feed;
        this.operations = operations;
        this.properties = properties;
    }

    public String getId() {
        return id;
    }

    public String getFeed() {
        return feed;
    }

    public Collection<Operation> getOperations() {
        return Collections.unmodifiableCollection(operations);
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }
}
