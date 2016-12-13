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
package org.hawkular.cmdgw.ws.test;

import okhttp3.Credentials;

/**
 * This provides some static configuration needed by the test client.
 * It has no Arquillian or other test framework dependencies.
 */
public class ClientConfig {

    protected static final String authentication;
    protected static final String baseGwUri;
    protected static final String host;
    protected static final String testPasword = System.getProperty("hawkular.itest.rest.password");
    protected static final String testUser = System.getProperty("hawkular.itest.rest.user");
    public static final String authHeader = Credentials.basic(testUser, testPasword);

    static {
        String h = System.getProperty("hawkular.bind.address", "localhost");
        if ("0.0.0.0".equals(h)) {
            h = "localhost";
        }
        host = h;
        int portOffset = Integer.parseInt(System.getProperty("hawkular.port.offset", "0"));
        int httpPort = portOffset + 8080;
        baseGwUri = "ws://" + host + ":" + httpPort + "/hawkular/command-gateway";
        authentication = "{\"username\":\"" + testUser + "\",\"password\":\"" + testPasword + "\"}";
    }

}
