/*
 * Copyright 2014-2016 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.jaxrs.filter.tenant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Juraci Paixão Kröhling
 */
@RunWith(Arquillian.class)
public class DefaultRequiresHeaderTest {
    @ArquillianResource
    URL baseUrl;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class)
                .addClass(TenantFeature.class)
                .addClass(TenantRequired.class)
                .addClass(TenantFilter.class)
                .addClass(FooEndpoint.class)
                .addClass(FooNoTenantEndpoint.class)
                .addClass(FooApp.class)
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void failsIfNoHeaderIsProvided() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("foo");
        Response response = target.request(MediaType.APPLICATION_JSON_TYPE).get();
        assertEquals(400, response.getStatus());
        assertTrue(response.readEntity(String.class).endsWith("has to be provided."));
    }

    @Test
    public void acceptsIfHeaderIsProvided() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("foo");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header("Hawkular-Tenant", "hawkular")
                .get();
        assertEquals(200, response.getStatus());
        assertEquals("bar", response.readEntity(String.class));
    }

    @Test
    public void acceptsIfHeaderIsNotProvidedOnNonRequiredMethod() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("foo");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity("empty", MediaType.APPLICATION_JSON_TYPE));
        assertEquals(200, response.getStatus());
        assertEquals("bar", response.readEntity(String.class));
    }

    @Test
    public void acceptsIfHeaderIsNotProvidedOnNonRequiredEndpoint() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("no-tenant");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
        assertEquals(200, response.getStatus());
        assertEquals("bar", response.readEntity(String.class));
    }

    @Test
    public void acceptsIfHeaderIsProvidedOnNonRequiredEndpoint() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(baseUrl.toString()).path("no-tenant");
        Response response = target
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header("Hawkular-Tenant", "hawkular")
                .get();
        assertEquals(200, response.getStatus());
        assertEquals("bar", response.readEntity(String.class));
    }
}
