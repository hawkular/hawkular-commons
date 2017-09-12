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

/**
 * @author Joel Takvorian
 */
public class Metric {

    private final String id;    // Unique index
    private final String name;
    private final String feed;    // Index; But not sure if feeds are still in play if the inventory is built from directly prometheus scans
    private final String typeId;  // Index [Search all metrics of type xx]

    // Lazy loaded
    private MetricType type;

    public Metric(String id, String name, String feed, String typeId) {
        this.id = id;
        this.name = name;
        this.feed = feed;
        this.typeId = typeId;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFeed() {
        return feed;
    }

    public String getTypeId() {
        return typeId;
    }

    public MetricType getType() {
        if (type == null) {
            // TODO: lazy load
        }
        return type;
    }
}
