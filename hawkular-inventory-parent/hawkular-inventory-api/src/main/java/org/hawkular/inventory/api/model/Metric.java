/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
    private String id;
    private String name;
    private String feed;    // Index; But not sure if feeds are still in play if the inventory is built from directly prometheus scans
    private MetricType type;  // Index [Search all metrics of type xx]
    private String root; // needed? Check if query exists "select all metrics for root"
}
