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
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

import org.hawkular.inventory.api.Import;
import org.hawkular.inventory.api.ResourceNode;
import org.hawkular.inventory.model.Metric;
import org.hawkular.inventory.model.MetricUnit;
import org.hawkular.inventory.model.Operation;
import org.hawkular.inventory.model.Resource;
import org.hawkular.inventory.model.ResourceType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
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
    private final Logger log = Logger.getLogger(InventoryRestTest.class);

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
    private static final Map<String, Map<String, String>> RELOAD_PARAMETERS;
    static {
        RELOAD_PARAMETERS = new HashMap<>();
        RELOAD_PARAMETERS.put("param1", new HashMap<>());
        RELOAD_PARAMETERS.get("param1").put("type", "bool");
        RELOAD_PARAMETERS.get("param1").put("description", "Description of param1 for Reload op");
        RELOAD_PARAMETERS.put("param2", new HashMap<>());
        RELOAD_PARAMETERS.get("param2").put("type", "bool");
        RELOAD_PARAMETERS.get("param2").put("description", "Description of param2 for Reload op");
    }
    private static final Map<String, Map<String, String>> SHUTDOWN_PARAMETERS;
    static {
        SHUTDOWN_PARAMETERS = new HashMap<>();
        SHUTDOWN_PARAMETERS.put("param1", new HashMap<>());
        SHUTDOWN_PARAMETERS.get("param1").put("type", "bool");
        SHUTDOWN_PARAMETERS.get("param1").put("description", "Description of param1 for Shutdown op");
        SHUTDOWN_PARAMETERS.put("param2", new HashMap<>());
        SHUTDOWN_PARAMETERS.get("param2").put("type", "bool");
        SHUTDOWN_PARAMETERS.get("param2").put("description", "Description of param2 for Shutdown op");
    }
    private static final Collection<Operation> EAP_OPS = Arrays.asList(
            new Operation("Reload", RELOAD_PARAMETERS),
            new Operation("Shutdown", SHUTDOWN_PARAMETERS),
            new Operation("Start", Collections.emptyMap()));
    private static final ResourceType TYPE_EAP = new ResourceType("EAP", EAP_OPS, new HashMap<>());

    private static final Import IMPORT = new Import(Arrays.asList(EAP1, EAP2, CHILD1, CHILD2, CHILD3, CHILD4),
            Collections.singletonList(TYPE_EAP));

    public static Import createResourceTypes() {
        Map<String, Map<String, String>> reload;
        reload = new HashMap<>();
        reload.put("param1", new HashMap<>());
        reload.get("param1").put("type", "bool");
        reload.get("param1").put("description", "Description of param1 for Reload op");
        reload.put("param2", new HashMap<>());
        reload.get("param2").put("type", "bool");
        reload.get("param2").put("description", "Description of param2 for Reload op");

        Map<String, Map<String, String>> shutdown;
        shutdown = new HashMap<>();
        shutdown.put("param1", new HashMap<>());
        shutdown.get("param1").put("type", "bool");
        shutdown.get("param1").put("description", "Description of param1 for Shutdown op");
        shutdown.put("param2", new HashMap<>());
        shutdown.get("param2").put("type", "bool");
        shutdown.get("param2").put("description", "Description of param2 for Shutdown op");
        Collection<Operation> eapOps = Arrays.asList(
                new Operation("Reload", reload),
                new Operation("Shutdown", shutdown),
                new Operation("Start", Collections.EMPTY_MAP));
        ResourceType eapType = new ResourceType("EAP", eapOps, new HashMap<>());

        Map<String, Map<String, String>> flush;
        flush = new HashMap<>();
        flush.put("param1", new HashMap<>());
        flush.get("param1").put("type", "bool");
        flush.get("param1").put("description", "Description of param1 for Flush op");
        flush.put("param2", new HashMap<>());
        flush.get("param2").put("type", "bool");
        flush.get("param2").put("description", "Description of param2 for Flush op");
        Collection<Operation> jdgOps = Arrays.asList(
                new Operation("Flush", flush),
                new Operation("Delete", Collections.EMPTY_MAP));
        ResourceType jdgType = new ResourceType("JDG", jdgOps, new HashMap<>());

        List<ResourceType> resourceTypes = Arrays.asList(eapType, jdgType);

        return new Import(null, resourceTypes);
    }

    public static Import createLargeInventory(int from, int to) {
        List<Resource> resources = new ArrayList<>();
        for (int i = from; i < to; i++) {
            String typeId = (i % 2 == 0) ? "EAP" : "JDG";
            String id = "Server-" + i;
            String name = "Server " + typeId + " with Id " + id;
            String childId1 = id + "-child-1";
            String childId2 = id + "-child-2";
            String childName1 = "Child 1 from " + id;
            String childName2 = "Child 2 from " + id;

            Metric metric1 = new Metric("memory", "Memory", MetricUnit.BYTES, 10, new HashMap<>());
            Metric metric2 = new Metric("gc", "GC", MetricUnit.NONE, 10, new HashMap<>());

            Map<String, String> propsX = new HashMap<>();
            propsX.put("description", "This is a description for " + id);
            Resource serverX = new Resource(id,
                    name,
                    typeId,
                    null,
                    Arrays.asList(childId1, childId2),
                    Arrays.asList(metric1, metric2),
                    propsX);
            Resource child1 = new Resource(childId1, childName1, "FOO", id,
                    new ArrayList<>(), new ArrayList<>(), new HashMap<>());
            Resource child2 = new Resource(childId2, childName2, "BAR", id,
                    new ArrayList<>(), new ArrayList<>(), new HashMap<>());

            resources.add(serverX);
            resources.add(child1);
            resources.add(child2);
        }

        return new Import(resources, null);
    }

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
                .addAsWebInfResource(new File("src/main/webapp/WEB-INF/beans.xml"));
    }

    @Test
    public void test000_statusIsUp() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("status");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void test001_importResources() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("import");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(IMPORT, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(200, response.getStatus());
    }

    @Test
    public void test002_shouldFindResourcesById() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resource/EAP-1");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertEquals("EAP-1", response.readEntity(Resource.class).getId());

        target = client.target(baseUrl.toString()).path("resource/EAP-2");
        response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertEquals("EAP-2", response.readEntity(Resource.class).getId());

        target = client.target(baseUrl.toString()).path("resource/child-1");
        response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertEquals("child-1", response.readEntity(Resource.class).getId());
    }

    @Test
    public void test003_shouldNotFindResourcesById() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resource/nada");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(404, response.getStatus());
    }

    @Test
    public void test004_shouldGetTopResources() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources/top");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertThat(response.readEntity(new GenericType<Collection<Resource>>() {}))
                .extracting(Resource::getId)
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
        assertThat(response.readEntity(new GenericType<Collection<ResourceType>>() {}))
                .extracting(ResourceType::getId)
                .containsExactly("EAP");
    }

    @Test
    public void test006_shouldGetAllEAPs() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources/type/EAP");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertThat(response.readEntity(new GenericType<Collection<Resource>>() {}))
                .extracting(Resource::getId)
                .containsOnly("EAP-1", "EAP-2");
    }

    @Test
    public void test007_shouldGetAllFOOs() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources/type/FOO");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertThat(response.readEntity(new GenericType<Collection<Resource>>() {}))
                .extracting(Resource::getId)
                .containsOnly("child-1", "child-3");
    }

    @Test
    public void test008_shouldGetNoNada() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resources/type/nada");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertThat(response.readEntity(new GenericType<Collection<Resource>>() {})).isEmpty();
    }

    @Test
    public void test009_shouldGetChildren() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resource/tree/EAP-1");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        ResourceNode tree = response.readEntity(ResourceNode.class);
        assertThat(tree.getChildren())
                .extracting(ResourceNode::getId)
                .containsExactly("child-1", "child-2");
    }

    @Test
    public void test010_shouldGetEmptyChildren() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("resource/tree/child-1");
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
        WebTarget target = client.target(baseUrl.toString()).path("resource/tree/nada");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(404, response.getStatus());
    }

    @Test
    public void test015_shouldFailOnDetectedCycle() {
        Resource corruptedParent = new Resource("CP", "CP", "FOO", "",
                Collections.singletonList("CC"), new ArrayList<>(), new HashMap<>());
        Resource corruptedChild = new Resource("CC", "CC", "BAR", "CP",
                Collections.singletonList("CP"), new ArrayList<>(), new HashMap<>());
        Import corruptedImport = new Import(Arrays.asList(corruptedParent, corruptedChild), null);

        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("import");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(corruptedImport, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(200, response.getStatus());
        client.close();

        client = ClientBuilder.newClient();
        target = client.target(baseUrl.toString()).path("resource/tree/CP");
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
    public void zzz_clean() {
        // FIXME: proper way for "AfterClass" with arquillian given there's non-static stuff needed?
        // Delete resources
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString());
        IMPORT.getResources().forEach(r -> target.path("resource/" + r.getId()).request().delete().close());
        IMPORT.getTypes().forEach(t -> target.path("type/" + t.getId()).request().delete().close());
        target.path("resource/CP").request().delete().close();
        target.path("resource/CC").request().delete().close();
    }

    @Test
    @Ignore
    public void test016_largeImport() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("import");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(createResourceTypes(), MediaType.APPLICATION_JSON_TYPE));
        assertEquals(200, response.getStatus());

        int maxIterations = 1000;
        int maxServersPerIteration = 1000;

        for (int i = 0; i < maxIterations; i++) {
            int from = i * maxServersPerIteration;
            int to = ((i + 1) * maxServersPerIteration) - 1;
            client = ClientBuilder.newClient();
            target = client.target(baseUrl.toString()).path("import");
            response = target
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(createLargeInventory(from, to), MediaType.APPLICATION_JSON_TYPE));
            assertEquals(200, response.getStatus());
            int mod = maxIterations > 100 ? 100 : 10;
            if ( i % mod == 0) {
                log.infof("Creating [%s] Servers", (i * maxServersPerIteration));
            }
        }
    }

}
