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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.hawkular.commons.json.JsonUtil;
import org.hawkular.inventory.Resources;
import org.hawkular.inventory.api.ResourceFilter;
import org.hawkular.inventory.api.model.Inventory;
import org.hawkular.inventory.api.model.InventoryHealth;
import org.hawkular.inventory.api.model.RawResource;
import org.hawkular.inventory.api.model.Resource;
import org.hawkular.inventory.api.model.ResourceNode;
import org.hawkular.inventory.api.model.ResourceType;
import org.hawkular.inventory.api.model.ResultSet;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.SingleFileStoreConfiguration;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Joel Takvorian
 */
public class InventoryServiceIspnTest {

    private static final String ISPN_CONFIG_LOCAL = "/hawkular-inventory-ispn-test.xml";
    private static final String SCRAPE_CONFIG_LOCAL = "/hawkular-inventory-prometheus-scrape-config.yaml";
    private static final String PROMETHEUS_SCRAPE_CONFIG = "target/prometheus";
    private static EmbeddedCacheManager CACHE_MANAGER;
    private final InventoryServiceIspn service;
    private final InventoryStats inventoryStats;

    static {
        try {
            CACHE_MANAGER = new DefaultCacheManager(InventoryServiceIspn.class.getResourceAsStream(ISPN_CONFIG_LOCAL));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @BeforeClass
    public static void initClass() {

    }

    public InventoryServiceIspnTest() throws IOException {
        Cache<String, Object> resource = CACHE_MANAGER.getCache("resource");
        Cache<String, Object> resourceType = CACHE_MANAGER.getCache("resource_type");
        resource.clear();
        resourceType.clear();
        inventoryStats = new InventoryStats(resource,
                resourceType,
                new File(((SingleFileStoreConfiguration) resource.getAdvancedCache()
                        .getCacheConfiguration()
                        .persistence()
                        .stores()
                        .iterator()
                        .next()).location()));
        inventoryStats.init();
        ScrapeConfig scrapeConfig = null;
        try {
            scrapeConfig = JsonUtil.getYamlMapper().readValue(InventoryServiceIspn.class.getResourceAsStream(SCRAPE_CONFIG_LOCAL), ScrapeConfig.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        File scrapeLocation = new File(PROMETHEUS_SCRAPE_CONFIG);
        scrapeLocation.mkdirs();
        service = new InventoryServiceIspn(resource, resourceType, getClass().getClassLoader().getResource("").getPath(), inventoryStats, scrapeConfig, scrapeLocation);
    }

    @Before
    public void setUp() {
        service.addResource(Resources.EAP1);
        service.addResource(Resources.EAP2);
        service.addResource(Resources.CHILD1);
        service.addResource(Resources.CHILD2);
        service.addResource(Resources.CHILD3);
        service.addResource(Resources.CHILD4);
        service.addResourceType(Resources.TYPE_EAP);
        service.addResourceType(Resources.TYPE_FOO);
        service.addResourceType(Resources.TYPE_BAR);
    }

    @Test
    public void shouldGetResourcesById() {
        Optional<Resource> eap1 = service.getResourceById("EAP-1");
        assertThat(eap1).isPresent()
                .map(Resource::getName)
                .hasValue("EAP-1");
        assertThat(eap1)
                .map(Resource::getType)
                .hasValueSatisfying(type -> assertThat(type.getId()).isEqualTo("EAP"));
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
        Collection<Resource> top = service.getResources(ResourceFilter.rootOnly().build()).getResults();
        assertThat(top)
                .extracting(Resource::getName)
                .containsOnly("EAP-1", "EAP-2");
        assertThat(top)
                .extracting(Resource::getType)
                .extracting(ResourceType::getId)
                .containsOnly("EAP", "EAP");
    }

    @Test
    public void shouldGetResourceTypes() {
        assertThat(service.getResourceTypes().getResults())
                .extracting(ResourceType::getId)
                .containsOnly("EAP", "FOO", "BAR");
    }

    @Test
    public void shouldGetAllEAPs() {
        assertThat(service.getResources(ResourceFilter.ofType("EAP").build()).getResults())
                .extracting(Resource::getId)
                .containsOnly("EAP-1", "EAP-2");
    }

    @Test
    public void shouldGetAllFOOs() {
        assertThat(service.getResources(ResourceFilter.ofType("FOO").build()).getResults())
                .extracting(Resource::getId)
                .containsOnly("child-1", "child-3");
    }

    @Test
    public void shouldGetNoNada() {
        assertThat(service.getResources(ResourceFilter.ofType("nada").build()).getResults()).isEmpty();
    }

    @Test
    public void shouldGetOnlyChildren() {
        assertThat(service.getChildren("EAP-1").getResults())
                .extracting(Resource::getId)
                .containsOnly("child-1", "child-2");
    }

    @Test
    public void shouldGetChildren() {
        ResourceNode tree = service.getTree("EAP-1").orElseThrow(AssertionError::new);
        assertThat(tree.getChildren())
                .extracting(ResourceNode::getId)
                .containsOnly("child-1", "child-2");
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
        RawResource corruptedParent = new RawResource("CP", "CP", "feedX", "FOO", "CC",
                new ArrayList<>(), new HashMap<>(), new HashMap<>());
        RawResource corruptedChild = new RawResource("CC", "CC", "feedX", "BAR", "CP",
                new ArrayList<>(), new HashMap<>(), new HashMap<>());
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
        assertThat(service.getJMXExporterConfig("wildfly-10")).isPresent()
                .hasValueSatisfying(s -> assertThat(s).contains("- pattern:"));
    }

    @Test
    public void shouldRemoveResource() {
        assertThat(service.getResourceById("EAP-1")).isPresent();
        service.deleteResources(Collections.singleton("EAP-1"));
        assertThat(service.getResourceById("EAP-1")).isNotPresent();
    }

    @Test
    public void shouldRemoveResourceType() {
        assertThat(service.getResourceType("EAP")).isPresent();
        service.deleteResourceTypes(Collections.singleton("EAP"));
        assertThat(service.getResourceType("EAP")).isNotPresent();
    }

    @Test
    public void shouldGetAllEAPsPerFeed() {
        assertThat(service.getResources(ResourceFilter.ofType("EAP").andFeed("feed1").build()).getResults())
                .extracting(Resource::getId)
                .containsOnly("EAP-1");

        assertThat(service.getResources(ResourceFilter.ofType("EAP").andFeed("feed2").build()).getResults())
                .extracting(Resource::getId)
                .containsOnly("EAP-2");
    }

    @Test
    public void createLargeSetAndFetchPagination() {
        int maxFeeds = 10;
        int maxItems = 1000;
        for (int j = 0; j < maxFeeds; j++) {
            List<RawResource> resources = new ArrayList<>();
            for (int i = 0; i < maxItems; i++) {
                RawResource resourceX = new RawResource("F" + j + "L" + i, "Large" + i, "feed" + j, "FOO", null,
                        new ArrayList<>(), new HashMap<>(), new HashMap<>());
                resources.add(resourceX);
            }
            service.addResource(resources);
        }

        ResultSet<Resource> results = service.getResources(ResourceFilter.ofType("FOO").build());
        assertThat(results.getResultSize()).isEqualTo(maxFeeds * maxItems + 2);
        assertThat(results.getResults().size()).isEqualTo(100);

        for (int i = 0; i < (maxItems / 100); i++) {
            results = service.getResources(ResourceFilter.ofType("FOO").build(),  i * 100, 100);
            assertThat(results.getResultSize()).isEqualTo(maxFeeds * maxItems + 2);
            assertThat(results.getResults().size()).isEqualTo(100);
            assertThat(results.getStartOffset()).isEqualTo(i * 100);
        }

        for (int j = 0; j < maxFeeds; j++) {
            results = service.getResources(ResourceFilter.ofType("FOO").andFeed("feed" + j).build(),  0, 1000);
            assertThat(results.getResults().size()).isEqualTo(1000);
        }
    }

    @Test
    public void shouldGetExport() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        service.buildExport(baos);
        byte[] bytes = baos.toByteArray();
        String str = new String(bytes);
        Inventory export = new ObjectMapper(new JsonFactory()).readValue(str, Inventory.class);
        assertThat(export).isNotNull();
        assertThat(export.getResources()).extracting(RawResource::getId).containsOnly("EAP-1", "EAP-2", "child-1",
                "child-2", "child-3", "child-4");
        assertThat(export.getTypes()).extracting(ResourceType::getId).containsOnly("EAP", "FOO", "BAR");
    }

    @Test
    public void shouldGetLargeExportWithStats() throws IOException, InterruptedException {
        int maxFeeds = 10;
        int maxItems = 1000;
        for (int j = 0; j < maxFeeds; j++) {
            List<RawResource> resources = new ArrayList<>();
            for (int i = 0; i < maxItems; i++) {
                RawResource resourceX = new RawResource("F" + j + "L" + i, "Large" + i, "feed" + j, "FOO", null,
                        new ArrayList<>(), new HashMap<>(), new HashMap<>());
                resources.add(resourceX);
            }
            service.addResource(resources);
        }
        int maxTypes = 200;
        List<ResourceType> resourceTypes = new ArrayList<>();
        for (int i = 0; i < maxTypes; i++) {
            ResourceType typeX = new ResourceType("RT" + i, new ArrayList<>(), new HashMap<>());
            resourceTypes.add(typeX);
        }
        service.addResourceType(resourceTypes);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        service.buildExport(baos);
        byte[] bytes = baos.toByteArray();
        String str = new String(bytes);
        Inventory export = new ObjectMapper(new JsonFactory()).readValue(str, Inventory.class);
        assertThat(export).isNotNull();
        assertThat(export.getResources())
                .hasSize(maxFeeds*maxItems + 6)
                .extracting(RawResource::getId)
                .contains("EAP-1", "F0L0", "F5L500", "F9L999");
        assertThat(export.getTypes())
                .hasSize(maxTypes + 3)
                .extracting(ResourceType::getId)
                .contains("EAP", "RT0", "RT100", "RT199");

        InventoryHealth health = service.getHealthStatus();
        assertThat(health.getInventoryStats()).isNotNull();
        assertThat(health.getInventoryStats().getNumberOfResources()).isGreaterThan(10000);
        assertThat(health.getInventoryStats().getNumberOfResourcesInMemory()).isEqualTo(5000);
        assertThat(health.getDiskStats().getInventoryTotalSpace()).isGreaterThan(4000000L);
    }

    @Test
    public void shouldDeleteAResourceAndCheckIsNotIndexed() throws IOException {
        String idXaDs = "itest-rest-feed~Local DMR~/subsystem=datasources/xa-data-source=testXaDs";
        String typeIdXaDs = "XA Datasource";
        String parentIdXaDs = "itest-rest-feed~Local DMR~~";
        String feedId = "itest-rest-feed";
        int numIterations = 1000;

        ResourceType xaDsType = ResourceType.builder().id(typeIdXaDs).build();
        service.addResourceType(xaDsType);

        for (int i = 0; i < numIterations; i++) {
            String idXaDsX = idXaDs + "-" + i;
            RawResource xaDs = RawResource.builder().id(idXaDsX)
                    .typeId(typeIdXaDs)
                    .parentId(parentIdXaDs)
                    .feedId(feedId)
                    .build();

            service.addResource(xaDs);

            Collection<Resource> resources = service.getResources(new ResourceFilter(false, feedId, null)).getResults();
            assertThat(resources)
                    .extracting(Resource::getId)
                    .contains(idXaDsX);

            service.deleteResources(Arrays.asList(idXaDsX));

            resources = service.getResources(new ResourceFilter(false, feedId, null)).getResults();
            assertThat(resources)
                    .extracting(Resource::getId)
                    .doesNotContain(idXaDsX);

            resources = service.getResources(new ResourceFilter(false, feedId, typeIdXaDs)).getResults();
            assertThat(resources)
                    .extracting(Resource::getId)
                    .doesNotContain(idXaDsX);
        }
    }

    @Test
    public void shouldGenerateScrapeConfig() {
        RawResource agent = RawResource.builder()
                .id("my-test-agent")
                .feedId("my-test-feed")
                .typeId("Hawkular WildFly Agent")
                .config("Metrics Endpoint", "localhost:1234")
                .build();
        service.addResource(agent);
        Assert.assertTrue(new File(PROMETHEUS_SCRAPE_CONFIG, "my-test-feed.json").exists());
    }
}
