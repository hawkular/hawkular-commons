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
import static org.assertj.core.data.MapEntry.entry;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class JsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void deserializeResultSet() throws Exception {
        int maxItems = 10;
        List<ResourceNode> resources = new ArrayList<>();
        List<Resource> resourcesWithType = new ArrayList<>();
        List<ResourceType> resourceTypes = new ArrayList<>();
        ResourceType fooType = new ResourceType("FOO", new HashSet<>(), new HashMap<>());
        for (int i = 0; i < maxItems; i++) {
            ResourceNode resourceX = new ResourceNode("L" + i, "Large" + i, "feedX", fooType, null, new ArrayList<>(),
                    new HashMap<>(), new HashMap<>(), new ArrayList<>());
            resources.add(resourceX);
            Resource resourceWTX = new Resource("Lbis" + i, "Largebis" + i, "feedX", fooType, null, new ArrayList<>(),
                    new HashMap<>(), new HashMap<>());
            resourcesWithType.add(resourceWTX);
            ResourceType resourceTypeX = new ResourceType("EAP" + i, new HashSet<>(), new HashMap<>());
            resourceTypes.add(resourceTypeX);
        }

        ResultSet<ResourceNode> rsResource = new ResultSet<>(resources, 10L, 0L);
        ResultSet<Resource> rsResourceWT = new ResultSet<>(resourcesWithType, 10L, 0L);
        ResultSet<ResourceType> rsResourceType = new ResultSet<>(resourceTypes, 10L, 0L);

        String jsonRsResource = objectMapper.writeValueAsString(rsResource);
        String jsonRsResourceWT = objectMapper.writeValueAsString(rsResourceWT);
        String jsonRsResourceType = objectMapper.writeValueAsString(rsResourceType);

        ResultSet<ResourceNode> dsRsResource = objectMapper.readValue(jsonRsResource, ResultSet.class);
        ResultSet<Resource> dsRsResourceWT = objectMapper.readValue(jsonRsResourceWT, ResultSet.class);
        ResultSet<ResourceType> dsRsResourceType = objectMapper.readValue(jsonRsResourceType, ResultSet.class);

        assertThat(rsResource.getResults())
                .usingElementComparator(Comparator.comparing(ResourceNode::getId))
                .isEqualTo(dsRsResource.getResults());
        assertThat(rsResourceWT.getResults())
                .usingElementComparator(Comparator.comparing(Resource::getId))
                .isEqualTo(dsRsResourceWT.getResults());
        assertEquals(rsResourceType.getResults(), dsRsResourceType.getResults());
    }

    @Test
    public void deserializeRawResource() throws Exception {
        RawResource resource = new RawResource("ID", "FOO", "FEED", "BAR", null,
                Collections.singletonList(
                        new Metric("metric", "m.metric", MetricUnit.BYTES, "sum(m.metric)", new HashMap<>(),
                                new HashMap<>())),
                Collections.singletonMap("r.prop", "r.val1"),
                Collections.singletonMap("r.cfg", "r.val2"));

        String jsonResource = objectMapper.writeValueAsString(resource);

        RawResource deserialized = objectMapper.readValue(jsonResource, RawResource.class);

        assertThat(deserialized).isNotNull();
        assertThat(deserialized.getId()).isEqualTo("ID");
        assertThat(deserialized.getMetrics()).extracting(Metric::getDisplayName).containsExactly("metric");
        assertThat(deserialized.getMetrics()).extracting(Metric::getExpression).containsExactly("sum(m.metric)");
        assertThat(deserialized.getProperties()).containsOnly(entry("r.prop", "r.val1"));
        assertThat(deserialized.getConfig()).containsOnly(entry("r.cfg", "r.val2"));
    }
}
