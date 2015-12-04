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
package org.hawkular.bus.restclient;

import org.hawkular.bus.restclient.RestClient.Type;
import org.junit.Assert;
import org.junit.Test;

public class RestClientTest {

    @Test
    public void testRestClientURL() throws Exception {
        RestClient client = new RestClient("localhost", null);
        Assert.assertEquals("http://localhost/hawkular-bus/message/", client.getEndpoint().toString());

        client = new RestClient("localhost", Integer.valueOf(-1));
        Assert.assertEquals("http://localhost/hawkular-bus/message/", client.getEndpoint().toString());

        client = new RestClient("localhost", Integer.valueOf(8080));
        Assert.assertEquals("http://localhost:8080/hawkular-bus/message/", client.getEndpoint().toString());

        client = new RestClient("https", "localhost", 80);
        Assert.assertEquals("https://localhost:80/hawkular-bus/message/", client.getEndpoint().toString());
        Assert.assertEquals("https://localhost:80/hawkular-bus/message/MyQueue?type=queue",
                client.getEndpointForType(Type.QUEUE, "MyQueue").toString());
        Assert.assertEquals("https://localhost:80/hawkular-bus/message/MyTopic?type=topic",
                client.getEndpointForType(Type.TOPIC, "MyTopic").toString());
    }
}
