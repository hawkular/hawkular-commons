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
package org.hawkular.bus.common;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EndpointTest {
    @Test
    public void endpointFromString() throws Exception {
        Endpoint e = new Endpoint("queue://the-name");
        assertTrue(e.getType() == Endpoint.Type.QUEUE);
        assertTrue(e.getName().equals("the-name"));

        e = new Endpoint("topic://another-name");
        assertTrue(e.getType() == Endpoint.Type.TOPIC);
        assertTrue(e.getName().equals("another-name"));

        try {
            new Endpoint(null);
            assert false : "should have failed";
        } catch (IllegalArgumentException expected) {
        }

        try {
            new Endpoint("foo://name");
            assert false : "should have failed";
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void endpointEquality() {
        Endpoint q1 = new Endpoint(Endpoint.Type.QUEUE, "foo");
        Endpoint q1Dup = new Endpoint(Endpoint.Type.QUEUE, "foo");
        Endpoint t1 = new Endpoint(Endpoint.Type.TOPIC, "foo");
        Endpoint t1Dup = new Endpoint(Endpoint.Type.TOPIC, "foo");
        Endpoint q2 = new Endpoint(Endpoint.Type.QUEUE, "bar");
        Endpoint t2 = new Endpoint(Endpoint.Type.TOPIC, "bar");
        assertTrue(q1.equals(q1Dup));
        assertTrue(t1.equals(t1Dup));

        assertFalse(q1.equals(q2));
        assertFalse(q1.equals(t1));
        assertFalse(q1.equals(t2));

        assertFalse(t1.equals(t2));
        assertFalse(t1.equals(q1));
        assertFalse(t1.equals(q2));
    }
}
