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
package org.hawkular.inventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hawkular.inventory.api.model.Inventory;
import org.hawkular.inventory.api.model.Metric;
import org.hawkular.inventory.api.model.MetricUnit;
import org.hawkular.inventory.api.model.Operation;
import org.hawkular.inventory.api.model.Resource;
import org.hawkular.inventory.api.model.ResourceType;

/**
 * @author Joel Takvorian
 */
public final class Resources {

    public static final Metric METRIC1
            = new Metric("memory1", "Memory", MetricUnit.BYTES, new HashMap<>());
    public static final Metric METRIC2
            = new Metric("gc1", "GC", MetricUnit.NONE, new HashMap<>());
    public static final Metric METRIC3
            = new Metric("memory2", "Memory", MetricUnit.BYTES, new HashMap<>());
    public static final Metric METRIC4
            = new Metric("gc2", "GC", MetricUnit.NONE, new HashMap<>());

    public static final Resource EAP1 = new Resource("EAP-1", "EAP-1", "feed1", "EAP", null,
            Arrays.asList(METRIC1, METRIC2), new HashMap<>(), new HashMap<>());
    public static final Resource EAP2 = new Resource("EAP-2", "EAP-2", "feed2", "EAP", null,
            Arrays.asList(METRIC3, METRIC4), new HashMap<>(), new HashMap<>());
    public static final Resource CHILD1 = new Resource("child-1", "Child 1", "feedX", "FOO", "EAP-1",
            new ArrayList<>(), new HashMap<>(), new HashMap<>());
    public static final Resource CHILD2 = new Resource("child-2", "Child 2", "feedX", "BAR", "EAP-1",
            new ArrayList<>(), new HashMap<>(), new HashMap<>());
    public static final Resource CHILD3 = new Resource("child-3", "Child 3", "feedX", "FOO", "EAP-2",
            new ArrayList<>(), new HashMap<>(), new HashMap<>());
    public static final Resource CHILD4 = new Resource("child-4", "Child 4", "feedX", "BAR", "EAP-2",
            new ArrayList<>(), new HashMap<>(), new HashMap<>());
    public static final Map<String, Map<String, String>> RELOAD_PARAMETERS;
    static {
        RELOAD_PARAMETERS = new HashMap<>();
        RELOAD_PARAMETERS.put("param1", new HashMap<>());
        RELOAD_PARAMETERS.get("param1").put("type", "bool");
        RELOAD_PARAMETERS.get("param1").put("description", "Description of param1 for Reload op");
        RELOAD_PARAMETERS.put("param2", new HashMap<>());
        RELOAD_PARAMETERS.get("param2").put("type", "bool");
        RELOAD_PARAMETERS.get("param2").put("description", "Description of param2 for Reload op");
    }
    public static final Map<String, Map<String, String>> SHUTDOWN_PARAMETERS;
    static {
        SHUTDOWN_PARAMETERS = new HashMap<>();
        SHUTDOWN_PARAMETERS.put("param1", new HashMap<>());
        SHUTDOWN_PARAMETERS.get("param1").put("type", "bool");
        SHUTDOWN_PARAMETERS.get("param1").put("description", "Description of param1 for Shutdown op");
        SHUTDOWN_PARAMETERS.put("param2", new HashMap<>());
        SHUTDOWN_PARAMETERS.get("param2").put("type", "bool");
        SHUTDOWN_PARAMETERS.get("param2").put("description", "Description of param2 for Shutdown op");
    }
    public static final Collection<Operation> EAP_OPS = Arrays.asList(
            new Operation("Reload", RELOAD_PARAMETERS),
            new Operation("Shutdown", SHUTDOWN_PARAMETERS),
            new Operation("Start", Collections.emptyMap()));
    public static final ResourceType TYPE_EAP = new ResourceType("EAP", EAP_OPS, new HashMap<>());
    public static final ResourceType TYPE_FOO = new ResourceType("FOO", Collections.emptyList(), new HashMap<>());
    public static final ResourceType TYPE_BAR = new ResourceType("BAR", Collections.emptyList(), new HashMap<>());

    public static final Inventory INVENTORY = new Inventory(Arrays.asList(EAP1, EAP2, CHILD1, CHILD2, CHILD3, CHILD4),
            Arrays.asList(TYPE_EAP, TYPE_FOO, TYPE_BAR));

    private Resources() {
    }
}
