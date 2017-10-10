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
package org.hawkular.inventory.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

import org.junit.Test;

/**
 * @author Joel Takvorian
 */
public class ResourceTest {

    private static final Metric METRIC1
            = new Metric("memory", "Memory", MetricUnit.BYTES, new HashMap<>());
    private static final Metric METRIC2
            = new Metric("gc", "GC", MetricUnit.NONE, new HashMap<>());

    private final Resource r = new Resource("id", "name", "feedX", "EAP",
            null, Arrays.asList(METRIC1, METRIC2), new HashMap<>());

    @Test
    public void shouldLazyLoadResourceType() {
        ResourceType eap = new ResourceType("EAP", new ArrayList<>(), new HashMap<>());
        LongAdder numberOfCalls = new LongAdder();
        Function<String, ResourceType> loader = id -> {
            numberOfCalls.increment();
            return eap;
        };

        ResourceType result = r.getType(loader);
        assertThat(result.getId()).isEqualTo("EAP");
        assertThat(numberOfCalls.intValue()).isEqualTo(1);
        // Verify loader is not called again
        r.getType(loader);
        assertThat(numberOfCalls.intValue()).isEqualTo(1);
    }

    @Test
    public void shouldLazyLoadChildren() {
        Resource child1 = new Resource("child-1", "name-1", "feedX", "t", "id",
                new ArrayList<>(), new HashMap<>());
        Resource child2 = new Resource("child-2", "name-2", "feedX", "t", "id",
                new ArrayList<>(), new HashMap<>());
        LongAdder numberOfCalls = new LongAdder();
        Function<String, List<Resource>> loader = id -> {
            numberOfCalls.increment();
            return Arrays.asList(child1, child2);
        };

        assertThat(r.getChildren(loader))
                .extracting(Resource::getName)
                .containsExactly("name-1", "name-2");
        assertThat(numberOfCalls.intValue()).isEqualTo(1);
        // Verify loader is not called again
        r.getChildren(loader);
        assertThat(numberOfCalls.intValue()).isEqualTo(1);
    }
}
