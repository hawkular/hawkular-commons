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

import java.util.Collections;
import java.util.Map;

/**
 * @author Joel Takvorian
 */
public class Metric {

    private final String id;    // Unique index ; Random UUID?
    private final String name;  // Prometheus short name? This field may not be necessary, name could just be a metadata put in properties
    private final String type;  // Ex: Deployment status, Server availability
    private final String feed;    // Index; But not sure if feeds are still in play if the inventory is built from directly prometheus scans
    private final MetricUnit unit;
    private final int collectionInterval;   // Not sure if we can get it from prometheus
    private final Map<String, String> properties;   // properties may contain, for instance, the full prometheus metric name

    public Metric(String id, String name, String type, String feed, MetricUnit unit,
                  int collectionInterval, Map<String, String> properties) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.feed = feed;
        this.unit = unit;
        this.collectionInterval = collectionInterval;
        this.properties = properties;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getFeed() {
        return feed;
    }

    public MetricUnit getUnit() {
        return unit;
    }

    public int getCollectionInterval() {
        return collectionInterval;
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }
}
