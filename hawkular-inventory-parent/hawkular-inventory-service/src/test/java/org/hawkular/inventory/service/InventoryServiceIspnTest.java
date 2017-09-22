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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import org.hawkular.inventory.api.ResourceNode;
import org.hawkular.inventory.model.Metric;
import org.hawkular.inventory.model.MetricUnit;
import org.hawkular.inventory.model.Operation;
import org.hawkular.inventory.model.Resource;
import org.hawkular.inventory.model.ResourceType;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Joel Takvorian
 */
public class InventoryServiceIspnTest {
    private static final Metric METRIC1
            = new Metric("memory1", "Memory", MetricUnit.BYTES, 10, new HashMap<>());
    private static final Metric METRIC2
            = new Metric("gc1", "GC", MetricUnit.NONE, 10, new HashMap<>());
    private static final Metric METRIC3
            = new Metric("memory2", "Memory", MetricUnit.BYTES, 10, new HashMap<>());
    private static final Metric METRIC4
            = new Metric("gc2", "GC", MetricUnit.NONE, 10, new HashMap<>());
    private static final Resource EAP1 = new Resource("EAP-1", "EAP-1", "EAP", null,
            Arrays.asList("child-1", "child-2"), Arrays.asList(METRIC1, METRIC2), new HashMap<>());
    private static final Resource EAP2 = new Resource("EAP-2", "EAP-2", "EAP", null,
            Arrays.asList("child-3", "child-4"), Arrays.asList(METRIC3, METRIC4), new HashMap<>());
    private static final Resource CHILD1 = new Resource("child-1", "Child 1", "FOO", "EAP-1",
            new ArrayList<>(), new ArrayList<>(), new HashMap<>());
    private static final Resource CHILD2 = new Resource("child-2", "Child 2", "BAR", "EAP-1",
            new ArrayList<>(), new ArrayList<>(), new HashMap<>());
    private static final Resource CHILD3 = new Resource("child-3", "Child 3", "FOO", "EAP-2",
            new ArrayList<>(), new ArrayList<>(), new HashMap<>());
    private static final Resource CHILD4 = new Resource("child-4", "Child 4", "BAR", "EAP-2",
            new ArrayList<>(), new ArrayList<>(), new HashMap<>());
    private static final Collection<Operation> EAP_OPS = Arrays.asList(
            new Operation("Reload", new HashMap<>()),
            new Operation("Shutdown", new HashMap<>()));
    private static final ResourceType TYPE_EAP = new ResourceType("EAP", EAP_OPS, new HashMap<>());

    private static final String ISPN_CONFIG_LOCAL = "/hawkular-inventory-ispn-test.xml";
    private static EmbeddedCacheManager CACHE_MANAGER;
    private final InventoryServiceIspn service;
    private final Cache<String, Object> backend;

    static {
        try {
            CACHE_MANAGER = new DefaultCacheManager(InventoryServiceIspn.class.getResourceAsStream(ISPN_CONFIG_LOCAL));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public InventoryServiceIspnTest() throws IOException {
        backend = CACHE_MANAGER.getCache("backend");
        backend.clear();
        service = new InventoryServiceIspn(backend, getClass().getClassLoader().getResource("").getPath());
    }

    @Before
    public void setUp() {
        service.addResource(EAP1);
        service.addResource(EAP2);
        service.addResource(CHILD1);
        service.addResource(CHILD2);
        service.addResource(CHILD3);
        service.addResource(CHILD4);
        service.addResourceType(TYPE_EAP);
    }

    @Test
    public void shouldGetResourcesById() {
        assertThat(service.getResourceById("EAP-1")).isPresent()
                .map(Resource::getName)
                .hasValue("EAP-1");
        assertThat(service.getResourceById("EAP-2")).isPresent()
                .map(Resource::getName)
                .hasValue("EAP-2");
        assertThat(service.getResourceById("child-1")).isPresent()
                .map(Resource::getName)
                .hasValue("Child 1");
    }

    @Test
    public void shouldNotGetResourcesById() {
        assertThat(service.getResourceById("nada")).isNotPresent();
    }

    @Test
    public void shouldGetTopResources() {
        assertThat(service.getAllTopResources())
                .extracting(Resource::getName)
                .containsOnly("EAP-1", "EAP-2");
    }

    @Test
    public void shouldGetResourceTypes() {
        assertThat(service.getAllResourceTypes())
                .extracting(ResourceType::getId)
                .containsExactly("EAP");
    }

    @Test
    public void shouldGetAllEAPs() {
        assertThat(service.getResourcesByType("EAP"))
                .extracting(Resource::getId)
                .containsOnly("EAP-1", "EAP-2");
    }

    @Test
    public void shouldGetAllFOOs() {
        assertThat(service.getResourcesByType("FOO"))
                .extracting(Resource::getId)
                .containsOnly("child-1", "child-3");
    }

    @Test
    public void shouldGetNoNada() {
        assertThat(service.getResourcesByType("nada")).isEmpty();
    }

    @Test
    public void shouldGetChildren() {
        ResourceNode tree = service.getTree("EAP-1").orElseThrow(AssertionError::new);
        assertThat(tree.getChildren())
                .extracting(ResourceNode::getId)
                .containsExactly("child-1", "child-2");
    }

    @Test
    public void shouldGetEmptyChildren() {
        ResourceNode tree = service.getTree("child-1").orElseThrow(AssertionError::new);
        assertThat(tree.getChildren()).isEmpty();
    }

    @Test
    public void shouldNotGetTree() {
        assertThat(service.getTree("nada")).isNotPresent();
    }

    @Test
    public void shouldGetResourceType() {
        assertThat(service.getResourceType("EAP")).isPresent()
                .map(ResourceType::getId)
                .hasValue("EAP");
    }

    @Test
    public void shouldNotGetResourceType() {
        assertThat(service.getResourceType("nada")).isNotPresent();
    }

    @Test
    public void shouldFailOnDetectedCycle() {
        Resource corruptedParent = new Resource("CP", "CP", "FOO", "",
                Collections.singletonList("CC"), new ArrayList<>(), new HashMap<>());
        Resource corruptedChild = new Resource("CC", "CC", "BAR", "CP",
                Collections.singletonList("CP"), new ArrayList<>(), new HashMap<>());
        service.addResource(corruptedParent);
        service.addResource(corruptedChild);

        assertThatThrownBy(() -> service.getTree("CP"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cycle detected");
    }

    @Test
    public void shouldNotGetAgentConfig() {
        assertThat(service.getAgentConfig("nada")).isNotPresent();
    }

    @Test
    public void shouldGetAgentConfig() throws IOException {
        assertThat(service.getAgentConfig("test")).isPresent()
            .hasValueSatisfying(s -> assertThat(s).contains("AGENT CONFIG TEST"));
    }

    @Test
    public void shouldNotGetJMXExporterConfig() {
        assertThat(service.getJMXExporterConfig("nada")).isNotPresent();
    }

    @Test
    public void shouldGetJMXExporterConfig() throws IOException {
        assertThat(service.getJMXExporterConfig("test")).isPresent()
                .hasValueSatisfying(s -> assertThat(s).contains("JMX EXPORTER TEST"));
    }

    @Test
    public void shouldRemoveResource() {
        assertThat(service.getResourceById("EAP-1")).isPresent();
        service.deleteResource("EAP-1");
        assertThat(service.getResourceById("EAP-1")).isNotPresent();
    }

    @Test
    public void shouldRemoveResourceType() {
        assertThat(service.getResourceType("EAP")).isPresent();
        service.deleteResourceType("EAP");
        assertThat(service.getResourceType("EAP")).isNotPresent();
    }
}
