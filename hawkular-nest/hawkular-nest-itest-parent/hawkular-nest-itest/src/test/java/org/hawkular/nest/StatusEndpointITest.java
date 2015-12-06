/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.nest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 */
public class StatusEndpointITest {

    private static final String statusUrl = "http://127.0.0.1:8080/hawkular/nest/itest/status";
    private static final String itestWarPath = System.getProperty("hawkular.nest.itest.war.path");

    @Test
    public void testStatusEndpoint() throws IOException, InterruptedException {

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .addHeader("Accept", "application/json")
                .url(statusUrl)
                .build();

        Response response = client.newCall(request).execute();
        if (response.isSuccessful()) {
            String foundBody = response.body().string();

            Manifest manifest = readManifest(itestWarPath);
            Attributes attributes = manifest.getMainAttributes();

            String expected = String.format("{\"Implementation-Version\":\"%s\"," //
                    + "\"Built-From-Git-SHA1\":\"%s\","//
                    + "\"testKey1\":\"testValue1\"}", //
                    attributes.getValue("Implementation-Version"),
                    attributes.getValue("Built-From-Git-SHA1"));
            Assert.assertEquals(expected, foundBody);
        } else {
            Assert.fail("Could not get [" + statusUrl + "]: " + response.code() + " " + response.message());
        }

    }

    private static Manifest readManifest(String warPath) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(new File(warPath)))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if ("META-INF/MANIFEST.MF".equals(entry.getName())) {
                    return new Manifest(zip);
                }
            }
        }
        throw new IllegalStateException("No META-INF/MANIFEST.MF in [" + warPath + "]");
    }

}
