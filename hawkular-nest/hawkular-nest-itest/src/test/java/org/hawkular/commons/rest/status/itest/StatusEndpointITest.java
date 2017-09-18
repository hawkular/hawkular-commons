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
package org.hawkular.commons.rest.status.itest;

import java.io.File;
import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.impl.base.exporter.zip.ZipExporterImpl;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.testng.Assert;
import org.testng.annotations.Test;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 */
public class StatusEndpointITest extends Arquillian {
    public static final String GROUP = "rest-status";

    private static final String statusUrl = "http://127.0.0.1:8080/hawkular/nest/itest/status";
    private static final String shrinkwrapMavenSettings = System.getProperty("shrinkwrap.maven.settings");

    @Deployment
    public static WebArchive createDeployment() {
        File[] libs = Maven.configureResolver().fromFile(shrinkwrapMavenSettings).loadPomFromFile("pom.xml")
                .resolve("org.hawkular.commons:hawkular-rest-status", "com.squareup.okhttp3:okhttp")
                .withTransitivity().asFile();
        WebArchive archive = ShrinkWrap.create(WebArchive.class, StatusEndpointITest.class.getSimpleName() + ".war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(
                        StatusEndpointITest.class.getResource("/rest-status/jboss-deployment-structure.xml"),
                        "jboss-deployment-structure.xml")
                .addAsWebInfResource(
                        StatusEndpointITest.class.getResource("/rest-status/jboss-web.xml"),
                        "jboss-web.xml")
                .addAsWebInfResource(
                        StatusEndpointITest.class.getResource("/rest-status/web.xml"),
                        "web.xml")
                .addAsManifestResource(
                        StatusEndpointITest.class.getResource("/rest-status/MANIFEST.MF"),
                        "MANIFEST.MF")
                .addPackage(StatusEndpointITest.class.getPackage())
                .addAsLibraries(libs);
        ZipExporter exporter = new ZipExporterImpl(archive);
        exporter.exportTo(new File("target", StatusEndpointITest.class.getSimpleName() + ".war"));
        return archive;
    }

    @Test(groups = { GROUP })
    public void testStatusEndpoint() throws IOException, InterruptedException {

        OkHttpClient client = new OkHttpClient.Builder().build();

        Request request = new Request.Builder()
                .addHeader("Accept", "application/json")
                .url(statusUrl)
                .build();

        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            String foundBody = response.body().string();

            /* see src/test/resources/rest-status/MANIFEST.MF */
            String expected = "{\"Implementation-Version\":\"1.2.3.4\","//
                    + "\"Built-From-Git-SHA1\":\"cofeebabe\","//
                    + "\"testKey1\":\"testValue1\"}";
            Assert.assertEquals(foundBody, expected);
        } else {
            Assert.fail("Could not get [" + statusUrl + "]: " + response.code() + " " + response.message());
        }

    }

}
