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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hawkular.inventory.Resources;
import org.hawkular.inventory.api.model.Inventory;
import org.hawkular.inventory.api.model.Metric;
import org.hawkular.inventory.api.model.MetricUnit;
import org.hawkular.inventory.api.model.Operation;
import org.hawkular.inventory.api.model.Resource;
import org.hawkular.inventory.api.model.ResourceType;
import org.hawkular.inventory.api.model.ResultSet;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@RunWith(Arquillian.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@Category(Performance.class)
public class InventoryPerfTest {
    private final Logger log = Logger.getLogger(InventoryPerfTest.class);

    public static Inventory createResourceTypes() {
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
                new Operation("Start", Collections.emptyMap()));
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
                new Operation("Delete", Collections.emptyMap()));
        ResourceType jdgType = new ResourceType("JDG", jdgOps, new HashMap<>());

        List<ResourceType> resourceTypes = Arrays.asList(eapType, jdgType);

        return new Inventory(null, resourceTypes);
    }

    public static Inventory createLargeInventory(int from, int to, int children, int metrics) {
        List<Resource> resources = new ArrayList<>();
        for (int i = from; i < to; i++) {
            String typeId = (i % 2 == 0) ? "EAP" : "JDG";
            String id = "Server-" + i;
            String name = "Server " + typeId + " with Id " + id;
            String feedX = "feedX";
            List<Resource> childrenResource = new ArrayList<>();
            List<String> childrenIds = new ArrayList<>();
            for (int j = 0; j < children; j++) {
                String childType = (j % 2 == 0) ? "FOO" : "BAR";
                String childIdX = id + "-child-" + j;
                String childNameX = "Child "+ j + " from " + id;
                childrenIds.add(childIdX);
                Resource childX = new Resource(childIdX, childNameX, feedX, childType, id,
                        new ArrayList<>(), new HashMap<>(), new HashMap<>());

                childrenResource.add(childX);
            }

            List<Metric> metricsResource = new ArrayList<>();
            for (int k = 0; k < metrics; k++) {
                Metric metricX = new Metric("metric-" + k, "Metric " + k, MetricUnit.BYTES, new HashMap<>());
                metricsResource.add(metricX);
            }

            Map<String, String> propsX = new HashMap<>();
            propsX.put("description", "This is a description for " + id);
            Resource serverX = new Resource(id,
                    name,
                    feedX,
                    typeId,
                    null,
                    metricsResource,
                    propsX,
                    new HashMap<>());

            resources.add(serverX);
            resources.addAll(childrenResource);
        }

        return new Inventory(resources, null);
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
                .addAsResource(new File("src/main/resources/wildfly-10-jmx-exporter.yml"))
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
    public void test002_largeImport() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("import");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(createResourceTypes(), MediaType.APPLICATION_JSON_TYPE));
        assertEquals(200, response.getStatus());

        int maxIterations = 50;
        int maxServersPerIteration = 100;
        int childrenPerServer = 100;
        int metricsPerServer = 20;

        for (int i = 0; i < maxIterations; i++) {
            int from = i * maxServersPerIteration;
            int to = ((i + 1) * maxServersPerIteration) - 1;
            client = ClientBuilder.newClient();
            target = client.target(baseUrl.toString()).path("import");
            response = target
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(createLargeInventory(from, to, childrenPerServer, metricsPerServer), MediaType.APPLICATION_JSON_TYPE));
            assertEquals(200, response.getStatus());
            int mod = maxIterations > 500 ? 100 : 10;
            if ( i % mod == 0) {
                log.infof("Creating [%s] Servers", (i * maxServersPerIteration));
            }
        }
        log.infof("Final - Created [%s] Servers", (maxIterations * maxServersPerIteration));

        log.infof("Fetch top resources");
        for (int i = 0; i < maxIterations; i++) {
            client = ClientBuilder.newClient();
            target = client.target(baseUrl.toString())
                    .path("resources")
                    .queryParam("root", true)
                    .queryParam("maxResults", maxServersPerIteration)
                    .queryParam("startOffset", i * maxServersPerIteration);
            response = target
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .get();
            assertEquals(200, response.getStatus());
            int mod = maxIterations > 500 ? 100 : 10;
            if ( i % mod == 0) {
                log.infof("Querying [%s] Servers", (i * maxServersPerIteration));
            }
        }

        log.info("Querying all types");
        client = ClientBuilder.newClient();
        target = client.target(baseUrl.toString()).path("types");
        response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertThat((List<ResourceType>) response.readEntity(ResultSet.class).getResults())
                .extracting(ResourceType::getId)
                .containsOnly("EAP", "FOO", "BAR", "JDG");
    }

    @Test
    public void test003_shouldGetResourceTypesAndNotImpactFromResources() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("types");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .accept(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertThat((List<ResourceType>) response.readEntity(ResultSet.class).getResults())
                .extracting(ResourceType::getId)
                .containsOnly("EAP", "FOO", "BAR", "JDG");
    }

    @Test
    public void zzz_clean() {
        // FIXME: proper way for "AfterClass" with arquillian given there's non-static stuff needed?
        // Delete resources
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString());
        target.path("type/JDG").request().delete().close();
    }
}
