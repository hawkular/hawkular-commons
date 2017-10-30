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
import java.util.Optional;

import org.junit.Test;

/**
 * @author Joel Takvorian
 */
public class ResourceTest {

    private static final ResourceType EAP = new ResourceType("EAP", new ArrayList<>(), new HashMap<>());
    private static final Metric METRIC1
            = new Metric("Memory", "jvm.memory", MetricUnit.BYTES, new HashMap<>(), new HashMap<>());
    private static final Metric METRIC2
            = new Metric("GC", "jvm.gc", MetricUnit.NONE, new HashMap<>(), new HashMap<>());

    private static final RawResource RAW = new RawResource("id", "name", "feedX", "EAP",
            null, Arrays.asList(METRIC1, METRIC2), new HashMap<>(), new HashMap<>());

    @Test
    public void shouldCreateFromRaw() {
        Resource r = Resource.fromRaw(RAW, id -> Optional.of(EAP));
        assertThat(r.getType().getId()).isEqualTo("EAP");
        assertThat(r.getMetrics()).extracting(Metric::getDisplayName).containsExactly("Memory", "GC");
    }
}
