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

import java.util.Collections;

import org.junit.Test;

public class BuilderTest {

    @Test
    public void resourceTypeTest() {
        Operation o = Operation.builder()
                .name("op.name")
                .parameter("param.name1", Collections.singletonMap("param.nameA", "param.valueB"))
                .parameter("param.name2", Collections.singletonMap("param.nameY", "param.valueZ"))
                .build();
        ResourceType t = ResourceType.builder()
                .id("type.id")
                .operation(o)
                .property("type.prop1", "type.value1")
                .property("type.prop2", "type.value2")
                .build();

        assertThat(o.getName()).isEqualTo("op.name");
        assertThat(o.getParameters()).containsOnlyKeys("param.name1", "param.name2");

        assertThat(t.getId()).isEqualTo("type.id");
        assertThat(t.getProperties()).containsOnly(
                entry("type.prop1", "type.value1"),
                entry("type.prop2", "type.value2"));
        assertThat(t.getOperations()).contains(o);
    }

    @Test
    public void resourceTest() {
        Metric m = Metric.builder()
                .name("metric.name")
                .type("metric.type")
                .property("metric.prop", "metric.value")
                .unit(MetricUnit.HOURS)
                .build();
        Resource r = Resource.builder()
                .id("res.id")
                .name("res.name")
                .typeId("res.typeId")
                .parentId("res.parentId")
                .property("res.prop1", "res.value1")
                .property("res.prop2", "res.value2")
                .config("res.cfg1", "res.cfgval1")
                .metric(m)
                .build();

        assertThat(m.getName()).isEqualTo("metric.name");
        assertThat(m.getType()).isEqualTo("metric.type");
        assertThat(m.getUnit()).isEqualTo(MetricUnit.HOURS);
        assertThat(m.getProperties()).containsOnly(entry("metric.prop", "metric.value"));

        assertThat(r.getId()).isEqualTo("res.id");
        assertThat(r.getName()).isEqualTo("res.name");
        assertThat(r.getTypeId()).isEqualTo("res.typeId");
        assertThat(r.getParentId()).isEqualTo("res.parentId");
        assertThat(r.getProperties()).containsOnly(
                entry("res.prop1", "res.value1"),
                entry("res.prop2", "res.value2"));
        assertThat(r.getConfig()).containsOnly(
                entry("res.cfg1", "res.cfgval1"));
        assertThat(r.getMetrics().get(0)).isEqualTo(m);
    }

}
