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
package org.hawkular.inventory.handlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hawkular.inventory.Resources;
import org.hawkular.inventory.api.model.Inventory;
import org.hawkular.inventory.api.model.RawResource;
import org.hawkular.inventory.api.model.Resource;
import org.hawkular.inventory.api.model.ResourceNode;
import org.hawkular.inventory.api.model.ResourceType;
import org.hawkular.inventory.api.model.ResultSet;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@RunWith(Arquillian.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class InventoryRestTest extends AbstractInventoryITest {

    @Test
    @RunAsClient
    public void test0000_statusIsUp() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("status");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
    }

    @Test
    @RunAsClient
    public void test0001_clean() {
        WebTarget target = ClientBuilder.newClient().target(baseUrl.toString()).path("resources");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        assertEquals(200, response.getStatus());

        target = ClientBuilder.newClient().target(baseUrl.toString()).path("types");
        response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        assertEquals(200, response.getStatus());
    }

    @Test
    @RunAsClient
    public void test001_importResources() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("import");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(Resources.INVENTORY, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(200, response.getStatus());
    }

    @Test
    @RunAsClient
    public void test002_shouldFindResourcesById() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources/EAP-1");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        Resource resource = response.readEntity(Resource.class);
        assertEquals("EAP-1", resource.getId());
        assertEquals("EAP", resource.getType().getId());

        target = client.target(baseUrl.toString()).path("resources/EAP-2");
        response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertEquals("EAP-2", response.readEntity(Resource.class).getId());

        target = client.target(baseUrl.toString()).path("resources/child-1");
        response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertEquals("child-1", response.readEntity(Resource.class).getId());
    }

    @Test
    @RunAsClient
    public void test003_shouldNotFindResourcesById() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources/nada");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(404, response.getStatus());
    }

    @Test
    @RunAsClient
    public void test004_shouldGetTopResources() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources")
                .queryParam("root", true);
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        List<Resource> resources = (List<Resource>) response.readEntity(ResultSet.class).getResults();
        assertThat(resources)
                .extracting(Resource::getId)
                .containsOnly("EAP-1", "EAP-2");
    }

    @Test
    @RunAsClient
    public void test005_shouldGetResourceTypes() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("types");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertThat((List<ResourceType>) response.readEntity(ResultSet.class).getResults())
                .extracting(ResourceType::getId)
                .containsOnly("EAP", "FOO", "BAR");
    }

    @Test
    @RunAsClient
    public void test006_shouldGetAllEAPs() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources")
                .queryParam("typeId", "EAP");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertThat((List<Resource>) response.readEntity(ResultSet.class).getResults())
                .extracting(Resource::getId)
                .containsOnly("EAP-1", "EAP-2");
    }

    @Test
    @RunAsClient
    public void test007_shouldGetAllFOOs() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources")
                .queryParam("typeId", "FOO");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertThat((List<Resource>) response.readEntity(ResultSet.class).getResults())
                .extracting(Resource::getId)
                .containsOnly("child-1", "child-3");
    }

    @Test
    @RunAsClient
    public void test008_shouldGetNoNada() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources")
                .queryParam("typeId", "nada");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertThat(response.readEntity(ResultSet.class).getResults()).isEmpty();
    }

    @Test
    @RunAsClient
    public void test009_shouldGetChildren() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources/EAP-1/tree");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        ResourceNode tree = response.readEntity(ResourceNode.class);
        assertThat(tree.getChildren())
                .extracting(ResourceNode::getId)
                .containsOnly("child-1", "child-2");
    }

    @Test
    @RunAsClient
    public void test010_shouldGetEmptyChildren() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources/child-1/tree");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        ResourceNode tree = response.readEntity(ResourceNode.class);
        assertThat(tree.getChildren()).isEmpty();
    }

    @Test
    @RunAsClient
    public void test011_shouldNotGetTree() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources/nada/tree");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(404, response.getStatus());
    }

    @Test
    @RunAsClient
    public void test012_shouldGetOnlyChildren() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources/EAP-1/children");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertThat((List<Resource>) response.readEntity(ResultSet.class).getResults())
                .extracting(Resource::getId)
                .containsOnly("child-1", "child-2");
    }

    @Test
    @RunAsClient
    public void test015_shouldFailOnDetectedCycle() {
        RawResource corruptedParent = new RawResource("CP", "CP", "feedX", "FOO", "CC",
                new ArrayList<>(), new HashMap<>(), new HashMap<>());
        RawResource corruptedChild = new RawResource("CC", "CC", "feedX", "BAR", "CP",
                new ArrayList<>(), new HashMap<>(), new HashMap<>());
        Inventory corruptedInventory = new Inventory(Arrays.asList(corruptedParent, corruptedChild), null);

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("import");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(corruptedInventory, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(200, response.getStatus());
        client.close();

        client = ClientBuilder.newClient();
        target = client.target(baseUrl.toString()).path("resources/CP/tree");
        response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(500, response.getStatus());
        assertEquals("java.lang.IllegalStateException: Cycle detected in the tree with id CP; aborting operation. The inventory is invalid.", response.readEntity(Map.class).get("errorMsg"));
    }

    @Test
    @RunAsClient
    public void test016_shouldGetAgentConfig() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("get-inventory-config/test");
        Response response = target
                .request(MediaType.TEXT_PLAIN)
                .get();
        assertEquals(200, response.getStatus());
        assertThat(response.readEntity(new GenericType<String>() {}))
                .contains("AGENT CONFIG TEST");

        target = client.target(baseUrl.toString()).path("get-jmx-exporter-config/wildfly-10");
        response = target
                .request(MediaType.TEXT_PLAIN)
                .get();
        assertEquals(200, response.getStatus());
        assertThat(response.readEntity(new GenericType<String>() {}))
                .contains("- pattern:");
    }

    @Test
    @RunAsClient
    public void test017_shouldNotGetAgentConfig() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("get-inventory-config/nada");
        Response response = target
                .request(MediaType.TEXT_PLAIN)
                .get();
        assertEquals(404, response.getStatus());
    }

    @Test
    @RunAsClient
    public void test020_shouldGetAllEAPsPerFeed() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources")
                .queryParam("feedId", "feed1")
                .queryParam("typeId", "EAP");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertThat((List<Resource>) response.readEntity(ResultSet.class).getResults())
                .extracting(Resource::getId)
                .containsOnly("EAP-1");

        client = ClientBuilder.newClient();
        target = client.target(baseUrl.toString()).path("resources")
                .queryParam("feedId", "feed2")
                .queryParam("typeId", "EAP");
        response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertThat((List<Resource>) response.readEntity(ResultSet.class).getResults())
                .extracting(Resource::getId)
                .containsOnly("EAP-2");
    }

    @Test
    @RunAsClient
    public void test021_shouldGetExport() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("export");
        Response response = target
                .request(MediaType.APPLICATION_JSON)
                .get();
        assertThat(response.getStatus()).isEqualTo(200);
        Inventory imp = response.readEntity(Inventory.class);
        assertThat(imp).isNotNull();
        assertThat(imp.getResources()).extracting(RawResource::getId).containsOnly("EAP-1", "EAP-2", "child-1",
                "child-2", "child-3", "child-4", "CC", "CP");
        assertThat(imp.getTypes()).extracting(ResourceType::getId).containsOnly("EAP", "FOO", "BAR");
    }

    @Test
    @RunAsClient
    public void test022_shouldGetOneResourceType() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("types/EAP");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        ResourceType rt = response.readEntity(ResourceType.class);
        assertThat(rt).isNotNull();
        assertThat(rt.getId()).isEqualTo("EAP");
    }

    @Test
    @RunAsClient
    public void test023_shouldGetParent() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources/child-1/parent");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertThat(response.getStatus()).isEqualTo(200);
        Resource parent = response.readEntity(Resource.class);
        assertThat(parent).isNotNull();
        assertThat(parent.getId()).isEqualTo("EAP-1");
    }

    @Test
    @RunAsClient
    public void test024_shouldNotGetParentForRoot() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources/EAP-1/parent");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(response.hasEntity()).isFalse();
    }

    @Test
    @RunAsClient
    public void test100_shouldDeleteSeveralResources() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources")
                .queryParam("ids", "CC")
                .queryParam("ids", "CP");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        assertEquals(200, response.getStatus());

        target = ClientBuilder.newClient().target(baseUrl.toString()).path("resources/CC");
        response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(404, response.getStatus());

        target = ClientBuilder.newClient().target(baseUrl.toString()).path("resources/CP");
        response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(404, response.getStatus());

        // Check that not everything was deleted
        target = ClientBuilder.newClient().target(baseUrl.toString()).path("resources")
                .queryParam("root", true);
        response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        List<Resource> resources = (List<Resource>) response.readEntity(ResultSet.class).getResults();
        assertThat(resources)
                .extracting(Resource::getId)
                .containsOnly("EAP-1", "EAP-2");
    }

    @Test
    @RunAsClient
    public void test101_shouldDeleteAllResources() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        assertEquals(200, response.getStatus());

        target = ClientBuilder.newClient().target(baseUrl.toString()).path("resources")
                .queryParam("root", true);
        response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        List<Resource> resources = (List<Resource>) response.readEntity(ResultSet.class).getResults();
        assertThat(resources).isEmpty();
    }

    @Test
    @RunAsClient
    public void test102_shouldDeleteSeveralTypes() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("types")
                .queryParam("typeIds", "FOO")
                .queryParam("typeIds", "BAR");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        assertEquals(200, response.getStatus());

        target = ClientBuilder.newClient().target(baseUrl.toString()).path("types/FOO");
        response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(404, response.getStatus());

        target = ClientBuilder.newClient().target(baseUrl.toString()).path("types/BAR");
        response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(404, response.getStatus());

        // Check that not everything was deleted
        target = ClientBuilder.newClient().target(baseUrl.toString()).path("types");
        response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        List<ResourceType> resources = (List<ResourceType>) response.readEntity(ResultSet.class).getResults();
        assertThat(resources)
                .extracting(ResourceType::getId)
                .containsOnly("EAP");
    }

    @Test
    @RunAsClient
    public void test103_shouldDeleteAllTypes() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("types");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        assertEquals(200, response.getStatus());

        target = ClientBuilder.newClient().target(baseUrl.toString()).path("types");
        response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        List<ResourceType> types = (List<ResourceType>) response.readEntity(ResultSet.class).getResults();
        assertThat(types).isEmpty();
    }

    @Test
    @RunAsClient
    public void test104_shouldDeleteAResourceAndCheckIsNotIndexed() {
        String idXaDs = "itest-rest-feed~Local DMR~/subsystem=datasources/xa-data-source=testXaDs";
        String typeIdXaDs = "XA Datasource";
        String parentIdXaDs = "itest-rest-feed~Local DMR~~";
        String feedId = "itest-rest-feed";
        int numIterations = 1000;

        ResourceType xaDsType = ResourceType.builder().id(typeIdXaDs).build();
        Inventory types = new Inventory(null, Arrays.asList(xaDsType));

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("import");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(types, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(200, response.getStatus());

        for (int i = 0; i < numIterations; i++) {
            String idXaDsX = idXaDs + "-" + i;
            RawResource xaDs = RawResource.builder().id(idXaDsX)
                    .typeId(typeIdXaDs)
                    .parentId(parentIdXaDs)
                    .feedId(feedId)
                    .build();

            Inventory inventory = new Inventory(Arrays.asList(xaDs), null);

            client = ClientBuilder.newClient();
            target = client.target(baseUrl.toString()).path("import");
            response = target
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(inventory, MediaType.APPLICATION_JSON_TYPE));
            assertEquals(200, response.getStatus());

            client = ClientBuilder.newClient();
            target = client.target(baseUrl.toString())
                    .path("resources")
                    .queryParam("feedId", feedId);
            response = target
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get();
            assertEquals(200, response.getStatus());
            List<Resource> resources = (List<Resource>) response.readEntity(ResultSet.class).getResults();
            assertThat(resources)
                    .extracting(Resource::getId)
                    .contains(idXaDsX);

            target = client.target(baseUrl.toString())
                    .path("resources")
                    .queryParam("ids", idXaDsX);
            response = target
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .delete();
            assertEquals(200, response.getStatus());

            client = ClientBuilder.newClient();
            target = client.target(baseUrl.toString())
                    .path("resources")
                    .queryParam("feedId", feedId);
            response = target
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get();
            assertEquals(200, response.getStatus());
            resources = (List<Resource>) response.readEntity(ResultSet.class).getResults();
            assertThat(resources)
                    .extracting(Resource::getId)
                    .doesNotContain(idXaDs);

            client = ClientBuilder.newClient();
            target = client.target(baseUrl.toString())
                    .path("resources")
                    .queryParam("feedId", feedId)
                    .queryParam("typeId", typeIdXaDs);
            response = target
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get();
            assertEquals(200, response.getStatus());
            resources = (List<Resource>) response.readEntity(ResultSet.class).getResults();
            assertThat(resources)
                    .extracting(Resource::getId)
                    .doesNotContain(idXaDs);
        }
    }

    @Test
    @RunAsClient
    public void test105_shouldCreateAPrometheusJsonConfig() {
        String id = "my-test-agent";
        String feedId = "my-test-feed";
        String type = "Hawkular WildFly Agent";
        int numIterations = 1000;
        String testPrometheusConfig = System.getProperty("test.prometheus.config");

        for (int i = 0; i < numIterations; i++) {
            RawResource agent = RawResource.builder()
                    .id(id + "-" + i)
                    .feedId(feedId + "-" + i)
                    .typeId(type)
                    .config("Metrics Endpoint", "localhost:1234")
                    .build();

            Inventory inventory = new Inventory(Arrays.asList(agent), null);

            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(baseUrl.toString()).path("import");
            Response response = target
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(inventory, MediaType.APPLICATION_JSON_TYPE));
            assertEquals(200, response.getStatus());
            assertTrue(new File(testPrometheusConfig, feedId + "-" + i + ".json").exists());
        }
    }
}
