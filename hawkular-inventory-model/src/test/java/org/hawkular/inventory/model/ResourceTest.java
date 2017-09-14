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
package org.hawkular.inventory.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Joel Takvorian
 */
public class ResourceTest {

    private final Resource r = new Resource("id", "name", "EAP", null,
            Arrays.asList("child-1", "child-2"), Arrays.asList("m-1", "m-2"), new HashMap<>());

    @Test
    public void shouldLazyLoadResourceType() {
        ResourceType eap = new ResourceType("EAP", new ArrayList<>(), new HashMap<>());
        LongAdder numberOfCalls = new LongAdder();
        Function<String, ResourceType> loader = id -> {
            numberOfCalls.increment();
            return eap;
        };

        ResourceType result = r.getType(loader);
        Assert.assertEquals("EAP", result.getId());
        Assert.assertEquals(1, numberOfCalls.intValue());
        // Verify loader is not called again
        r.getType(loader);
        Assert.assertEquals(1, numberOfCalls.intValue());
    }

    @Test
    public void shouldLazyLoadChildren() {
        Resource child1 = new Resource("child-1", "name-1", "t", "id",
                new ArrayList<>(), new ArrayList<>(), new HashMap<>());
        Resource child2 = new Resource("child-2", "name-2", "t", "id",
                new ArrayList<>(), new ArrayList<>(), new HashMap<>());
        LongAdder numberOfCalls = new LongAdder();
        Function<String, Resource> loader = id -> {
            numberOfCalls.increment();
            if (id.equals(child1.getId())) {
                return child1;
            }
            return child2;
        };

        List<Resource> children = r.getChildren(loader);
        Assert.assertEquals("name-1", children.get(0).getName());
        Assert.assertEquals("name-2", children.get(1).getName());
        Assert.assertEquals(2, numberOfCalls.intValue());
        // Verify loader is not called again
        r.getChildren(loader);
        Assert.assertEquals(2, numberOfCalls.intValue());
    }

    @Test
    public void shouldLazyLoadMetrics() {
        Metric m1 = new Metric("m-1", "name-1", "mtype", MetricUnit.BYTES, 10,
                new HashMap<>());
        Metric m2 = new Metric("m-2", "name-2", "mtype", MetricUnit.BYTES, 10,
                new HashMap<>());
        LongAdder numberOfCalls = new LongAdder();
        Function<String, Metric> loader = id -> {
            numberOfCalls.increment();
            if (id.equals(m1.getId())) {
                return m1;
            }
            return m2;
        };

        List<Metric> metrics = r.getMetrics(loader);
        Assert.assertEquals("name-1", metrics.get(0).getName());
        Assert.assertEquals("name-2", metrics.get(1).getName());
        Assert.assertEquals(2, numberOfCalls.intValue());
        // Verify loader is not called again
        r.getMetrics(loader);
        Assert.assertEquals(2, numberOfCalls.intValue());
    }
}
