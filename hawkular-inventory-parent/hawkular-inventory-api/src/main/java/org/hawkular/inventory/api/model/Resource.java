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
import java.util.Map;

/**
 * @author Joel Takvorian
 */
public class Resource {
    private String id;  // Unique index [Search resource by id]
    private String name;
    private ResourceType type;  // Index [Search all resources of type xx]
    private String feed;    // Index; But not sure if feeds are still in play if the inventory is built from directly prometheus scans
    private String rootId;  // Nullable; Index [Search all resources under root xx]
    private Map<String, String> properties;

    // Maybe "children" and "metrics" should be removed from "Resource" and put in another class that represents the whole tree
    private Collection<Resource> children;
    private Collection<Metric> metrics;
}
