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
package org.hawkular.inventory.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.hawkular.commons.json.JsonUtil;
import org.junit.Test;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class ScrapeConfigTest {
    @Test
    public void testReadConfigYaml() throws Exception {
        ScrapeConfig config = JsonUtil.getYamlMapper().readValue(ScrapeConfigTest.class.getResourceAsStream("/hawkular-inventory-prometheus-scrape-config.yaml"), ScrapeConfig.class);
        assertEquals(1, config.getFilter().size());
        assertTrue(config.getFilter().containsKey("Hawkular Java Agent"));
        assertEquals("Metrics Endpoints", config.getFilter().get("Hawkular Java Agent"));
    }
}
