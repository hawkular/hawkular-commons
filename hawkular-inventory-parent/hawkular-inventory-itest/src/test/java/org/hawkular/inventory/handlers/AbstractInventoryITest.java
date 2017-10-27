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

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Assert;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public abstract class AbstractInventoryITest {

    private static final String BASE_URL = "hawkular-inventory.base-url";
    private static final String BASE_URL_DEFAULT = "http://localhost:8080/hawkular/inventory";

    protected URL baseUrl;

    public AbstractInventoryITest() {
        try {
            baseUrl = new URL(System.getProperty(BASE_URL, BASE_URL_DEFAULT));
        } catch (MalformedURLException e) {
            Assert.fail(e.getMessage());
        }
    }

    public URL getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URL baseUrl) {
        this.baseUrl = baseUrl;
    }
}
