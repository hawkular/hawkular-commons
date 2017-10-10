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

import java.io.File;
import java.net.URL;
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
import org.hawkular.inventory.api.model.Resource;
import org.hawkular.inventory.api.model.ResourceNode;
import org.hawkular.inventory.api.model.ResourceType;
import org.hawkular.inventory.api.model.ResourceWithType;
import org.hawkular.inventory.api.model.ResultSet;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
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
public class InventoryRestTest {

    @ArquillianResource
    private URL baseUrl;

    @Deployment
    public static WebArchive createDeployment() {
        File[] libs = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .importRuntimeDependencies()
                .resolve()
                .withTransitivity()
                .asFile();
        File[] assertj = Maven.resolver()
                .loadPomFromFile("pom.xml")
                .resolve("org.assertj:assertj-core")
                .withTransitivity()
                .asFile();
        return ShrinkWrap.create(WebArchive.class)
                .addPackages(true, "org.hawkular.inventory")
                .addAsLibraries(libs)
                .addAsLibraries(assertj)
                .setWebXML(new File("src/main/webapp/WEB-INF/web.xml"))
                .addAsResource(new File("src/main/resources/hawkular-inventory-ispn.xml"))
                .addAsResource(new File("src/main/resources/wildfly-10-jmx-exporter.yml"))
                .addAsWebInfResource(new File("src/main/webapp/WEB-INF/beans.xml"));
    }

    @Test
    public void test0000_statusIsUp() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("status");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
    }

    @Test
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
    public void test001_importResources() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("import");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(Resources.INVENTORY, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(200, response.getStatus());
    }

    @Test
    public void test002_shouldFindResourcesById() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources/EAP-1");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        ResourceWithType resource = response.readEntity(ResourceWithType.class);
        assertEquals("EAP-1", resource.getId());
        assertEquals("EAP", resource.getType().getId());

        target = client.target(baseUrl.toString()).path("resources/EAP-2");
        response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertEquals("EAP-2", response.readEntity(ResourceWithType.class).getId());

        target = client.target(baseUrl.toString()).path("resources/child-1");
        response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertEquals("child-1", response.readEntity(ResourceWithType.class).getId());
    }

    @Test
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
    public void test004_shouldGetTopResources() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources")
                .queryParam("root", true);
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        List<ResourceWithType> resources = (List<ResourceWithType>) response.readEntity(ResultSet.class).getResults();
        assertThat(resources)
                .extracting(ResourceWithType::getId)
                .containsOnly("EAP-1", "EAP-2");
    }

    @Test
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
    public void test006_shouldGetAllEAPs() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources")
                .queryParam("typeId", "EAP");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertThat((List<ResourceWithType>) response.readEntity(ResultSet.class).getResults())
                .extracting(ResourceWithType::getId)
                .containsOnly("EAP-1", "EAP-2");
    }

    @Test
    public void test007_shouldGetAllFOOs() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources")
                .queryParam("typeId", "FOO");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertThat((List<ResourceWithType>) response.readEntity(ResultSet.class).getResults())
                .extracting(ResourceWithType::getId)
                .containsOnly("child-1", "child-3");
    }

    @Test
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
    public void test012_shouldGetOnlyChildren() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources/EAP-1/children");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertThat((List<ResourceWithType>) response.readEntity(ResultSet.class).getResults())
                .extracting(ResourceWithType::getId)
                .containsOnly("child-1", "child-2");
    }

    @Test
    public void test015_shouldFailOnDetectedCycle() {
        Resource corruptedParent = new Resource("CP", "CP", "feedX", "FOO", "CC",
                new ArrayList<>(), new HashMap<>(), new HashMap<>());
        Resource corruptedChild = new Resource("CC", "CC", "feedX", "BAR", "CP",
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
    public void test017_shouldNotGetAgentConfig() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("get-inventory-config/nada");
        Response response = target
                .request(MediaType.TEXT_PLAIN)
                .get();
        assertEquals(404, response.getStatus());
    }

    @Test
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
        assertThat((List<ResourceWithType>) response.readEntity(ResultSet.class).getResults())
                .extracting(ResourceWithType::getId)
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
        assertThat((List<ResourceWithType>) response.readEntity(ResultSet.class).getResults())
                .extracting(ResourceWithType::getId)
                .containsOnly("EAP-2");
    }

    @Test
    public void test021_shouldGetExport() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("export");
        Response response = target
                .request(MediaType.APPLICATION_JSON)
                .get();
        assertThat(response.getStatus()).isEqualTo(200);
        Inventory imp = response.readEntity(Inventory.class);
        assertThat(imp).isNotNull();
        assertThat(imp.getResources()).extracting(Resource::getId).containsOnly("EAP-1", "EAP-2", "child-1",
                "child-2", "child-3", "child-4", "CC", "CP");
        assertThat(imp.getTypes()).extracting(ResourceType::getId).containsOnly("EAP", "FOO", "BAR");
    }

    @Test
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
        List<ResourceWithType> resources = (List<ResourceWithType>) response.readEntity(ResultSet.class).getResults();
        assertThat(resources)
                .extracting(ResourceWithType::getId)
                .containsOnly("EAP-1", "EAP-2");
    }

    @Test
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
        List<ResourceWithType> resources = (List<ResourceWithType>) response.readEntity(ResultSet.class).getResults();
        assertThat(resources).isEmpty();
    }

    @Test
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
}
