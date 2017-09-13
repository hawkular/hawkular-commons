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
package org.hawkular.inventory.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

import org.hawkular.inventory.model.Metric;
import org.hawkular.inventory.model.MetricUnit;
import org.hawkular.inventory.model.Operation;
import org.hawkular.inventory.model.Resource;
import org.hawkular.inventory.model.ResourceType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joel Takvorian
 */
public class InventoryServiceTest {

    private static final Resource EAP1 = new Resource("EAP-1", "EAP-1", "EAP", "feed", "",
            Arrays.asList("child-1", "child-2"), Arrays.asList("m-1", "m-2"), new HashMap<>());
    private static final Resource EAP2 = new Resource("EAP-2", "EAP-2", "EAP", "feed", "",
            Arrays.asList("child-3", "child-4"), Arrays.asList("m-3", "m-4"), new HashMap<>());
    private static final Resource CHILD1 = new Resource("child-1", "Child 1", "FOO", "feed", "EAP-1",
            new ArrayList<>(), new ArrayList<>(), new HashMap<>());
    private static final Resource CHILD2 = new Resource("child-2", "Child 2", "BAR", "feed", "EAP-1",
            new ArrayList<>(), new ArrayList<>(), new HashMap<>());
    private static final Resource CHILD3 = new Resource("child-3", "Child 3", "FOO", "feed", "EAP-2",
            new ArrayList<>(), new ArrayList<>(), new HashMap<>());
    private static final Resource CHILD4 = new Resource("child-4", "Child 4", "BAR", "feed", "EAP-2",
            new ArrayList<>(), new ArrayList<>(), new HashMap<>());
    private static final Collection<Operation> EAP_OPS = Arrays.asList(
            new Operation("Reload", new HashMap<>()),
            new Operation("Shutdown", new HashMap<>()));
    private static final ResourceType TYPE_EAP = new ResourceType("EAP", "feed", EAP_OPS, new HashMap<>());
    private static final Metric METRIC1
            = new Metric("m-1", "memory", "Memory", "feed", MetricUnit.BYTES, 10, new HashMap<>());
    private static final Metric METRIC2
            = new Metric("m-2", "gc", "GC", "feed", MetricUnit.NONE, 10, new HashMap<>());
    private static final Metric METRIC3
            = new Metric("m-3", "memory", "Memory", "feed", MetricUnit.BYTES, 10, new HashMap<>());
    private static final Metric METRIC4
            = new Metric("m-4", "gc", "GC", "feed", MetricUnit.NONE, 10, new HashMap<>());

    private InventoryService service = new InventoryService();

    @Before
    public void setUp() {
        service.addResource(EAP1);
        service.addResource(EAP2);
        service.addResource(CHILD1);
        service.addResource(CHILD2);
        service.addResource(CHILD3);
        service.addResource(CHILD4);
        service.addResourceType(TYPE_EAP);
        service.addMetric(METRIC1);
        service.addMetric(METRIC2);
        service.addMetric(METRIC3);
        service.addMetric(METRIC4);
        service.buildIndexes();
    }

    @Test
    public void shouldfindResourcesById() {
        // TODO: use assertj
        Resource r1 = service.findResourceById("EAP-1").get();
        Assert.assertEquals("EAP-1", r1.getName());
        Resource r2 = service.findResourceById("EAP-2").get();
        Assert.assertEquals("EAP-2", r2.getName());
        Resource r3 = service.findResourceById("child-1").get();
        Assert.assertEquals("Child 1", r3.getName());
    }

    @Test
    public void shouldGetTopResources() {
        // TODO: use assertj
        Collection<Resource> roots = service.getAllTopResources();
        Assert.assertEquals(2, roots.size());
        Assert.assertEquals("EAP-1", roots.iterator().next().getName());
    }

    @Test
    public void shouldGetResourceTypes() {
        // TODO: use assertj
        Collection<ResourceType> types = service.getAllResourceTypes();
        Assert.assertEquals(1, types.size());
        Assert.assertEquals("EAP", types.iterator().next().getId());
    }

    @Test
    public void shouldGetAllEAPs() {
        // TODO: use assertj
        Collection<Resource> resources = service.getResourcesByType("EAP");
        Assert.assertEquals(2, resources.size());
        Assert.assertEquals("EAP-1", resources.iterator().next().getId());
    }

    @Test
    public void shouldGetAllFOOs() {
        // TODO: use assertj
        Collection<Resource> resources = service.getResourcesByType("FOO");
        Assert.assertEquals(2, resources.size());
        Assert.assertEquals("child-1", resources.iterator().next().getId());
    }

    @Test
    public void shouldGetChildren() {
        // TODO: use assertj
        Optional<Collection<Resource>> children = service.getChildResources("EAP-1");
        Assert.assertTrue(children.isPresent());
        Assert.assertEquals(2, children.get().size());
        Assert.assertEquals("child-1", children.get().iterator().next().getId());
    }

    @Test
    public void shouldGetEmptyChildren() {
        // TODO: use assertj
        Optional<Collection<Resource>> children = service.getChildResources("child-1");
        Assert.assertTrue(children.isPresent());
        Assert.assertEquals(0, children.get().size());
    }

    @Test
    public void shouldNotGetChildren() {
        // TODO: use assertj
        Optional<Collection<Resource>> children = service.getChildResources("nada");
        Assert.assertFalse(children.isPresent());
    }

    @Test
    public void shouldGetMetrics() {
        // TODO: use assertj
        Optional<Collection<Metric>> metrics = service.getResourceMetrics("EAP-1");
        Assert.assertTrue(metrics.isPresent());
        Assert.assertEquals(2, metrics.get().size());
        Assert.assertEquals("m-1", metrics.get().iterator().next().getId());
    }

    @Test
    public void shouldGetEmptyMetrics() {
        // TODO: use assertj
        Optional<Collection<Metric>> metrics = service.getResourceMetrics("child-1");
        Assert.assertTrue(metrics.isPresent());
        Assert.assertEquals(0, metrics.get().size());
    }

    @Test
    public void shouldNotGetMetrics() {
        // TODO: use assertj
        Optional<Collection<Metric>> metrics = service.getResourceMetrics("nada");
        Assert.assertFalse(metrics.isPresent());
    }
}
