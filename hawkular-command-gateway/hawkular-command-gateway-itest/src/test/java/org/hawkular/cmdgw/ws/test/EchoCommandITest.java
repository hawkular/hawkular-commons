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
package org.hawkular.cmdgw.ws.test;

import org.hawkular.cmdgw.ws.test.TestWebSocketClient.Answer;
import org.hawkular.cmdgw.ws.test.TestWebSocketClient.ExpectedEvent.ExpectedFailure;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.testng.annotations.Test;

import okhttp3.Credentials;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */

public class EchoCommandITest extends AbstractCommandITest {
    public static final String GROUP = "EchoCommandITest";

    private static final String echoRequestTemplate = "EchoRequest={\"authentication\": " + ClientConfig.authentication
            + ", \"echoMessage\": \"%s\"}";
    private static final String echoResponseTemplate = "EchoResponse={\"reply\":\"ECHO [%s]\"}";

    /**
     * Tests the simplest echo request-response exchange.
     *
     * @throws Throwable
     */
    @RunAsClient
    @Test(groups = { GROUP })
    public void testEcho() throws Throwable {

        try (TestWebSocketClient testClient = TestWebSocketClient.builder()
                .url(ClientConfig.baseGwUri + "/ui/ws") //
                .expectWelcome(String.format(echoRequestTemplate, "Yodel Ay EEE Oooo")) //
                .expectText(String.format(echoResponseTemplate, "Yodel Ay EEE Oooo"), Answer.CLOSE) //
                .expectClose()
                .build()) {
            testClient.validate(10000);
        }
    }

    @RunAsClient
    @Test(groups = { GROUP })
    public void testWithoutAuth() throws Throwable {

        try (TestWebSocketClient testClient = TestWebSocketClient.builder()
                .url(ClientConfig.baseGwUri + "/ui/ws") //
                .authentication(null) //
                .expectMessage(ExpectedFailure.UNAUTHORIZED) //
                .build()) {
            testClient.validate(10000);
        }
    }

    @RunAsClient
    @Test(groups = { GROUP })
    public void testBadPassword() throws Throwable {
        try (TestWebSocketClient testClient = TestWebSocketClient.builder()
                .url(ClientConfig.baseGwUri + "/ui/ws") //
                .authentication(Credentials.basic(ClientConfig.testUser, "bad password")) //
                .expectMessage(ExpectedFailure.UNAUTHORIZED) //
                .build()) {
            testClient.validate(10000);
        }
    }

    @RunAsClient
    @Test(groups = { GROUP })
    public void testBadUserAndPassword() throws Throwable {
        try (TestWebSocketClient testClient = TestWebSocketClient.builder()
                .url(ClientConfig.baseGwUri + "/ui/ws") //
                .authentication(Credentials.basic("baduser", "bad password")) //
                .expectMessage(ExpectedFailure.UNAUTHORIZED) //
                .build()) {
            testClient.validate(10000);
        }
    }

}
