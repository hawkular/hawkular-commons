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
package org.hawkular.inventory.api;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.hawkular.inventory.model.Resource;
import org.hawkular.inventory.model.ResourceType;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class JsonTest {

    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void deserializeResultSet() throws Exception {
        int maxItems = 10;
        List<Resource> resources = new ArrayList<>();
        List<ResourceType> resourceTypes = new ArrayList<>();
        for (int i = 0; i < maxItems; i++) {
            Resource resourceX = new Resource("L" + i, "Large" + i, "FOO", null,
                    new ArrayList<>(), new ArrayList<>(), new HashMap<>());
            resources.add(resourceX);
            ResourceType resourceTypeX = new ResourceType("EAP", new HashSet<>(), new HashMap<>());
            resourceTypes.add(resourceTypeX);
        }

        ResultSet<Resource> rsResource = new ResultSet<>(resources, 10L, 0L);
        ResultSet<ResourceType> rsResourceType = new ResultSet<>(resourceTypes, 10L, 0L);

        String jsonRsResource = objectMapper.writeValueAsString(rsResource);
        String jsonRsResourceType = objectMapper.writeValueAsString(rsResourceType);

        ResultSet<Resource> dsRsResource = objectMapper.readValue(jsonRsResource, ResultSet.class);
        ResultSet<ResourceType> dsRsResourceType = objectMapper.readValue(jsonRsResourceType, ResultSet.class);

        assertEquals(rsResource.getResults(), dsRsResource.getResults());
        assertEquals(rsResourceType.getResults(), dsRsResourceType.getResults());
    }
}
