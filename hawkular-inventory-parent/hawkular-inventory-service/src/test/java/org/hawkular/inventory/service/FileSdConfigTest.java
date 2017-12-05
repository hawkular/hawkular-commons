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

import org.junit.Assert;
import org.junit.Test;

public class FileSdConfigTest {
    @Test
    public void testEntryFromString() throws Exception {
        FileSdConfig.Entry entry;

        entry = FileSdConfig.Entry.buildFromString("host:1234");
        Assert.assertEquals(1, entry.getTargets().size());
        Assert.assertEquals("host:1234", entry.getTargets().get(0));
        Assert.assertEquals(0, entry.getLabels().size());

        entry = FileSdConfig.Entry.buildFromString("host:1234{label1=value1}");
        Assert.assertEquals(1, entry.getTargets().size());
        Assert.assertEquals("host:1234", entry.getTargets().get(0));
        Assert.assertEquals(1, entry.getLabels().size());
        Assert.assertEquals("value1", entry.getLabels().get("label1"));

        entry = FileSdConfig.Entry.buildFromString("host:1234{label1=value1, label2=value2}");
        Assert.assertEquals(1, entry.getTargets().size());
        Assert.assertEquals("host:1234", entry.getTargets().get(0));
        Assert.assertEquals(2, entry.getLabels().size());
        Assert.assertEquals("value1", entry.getLabels().get("label1"));
        Assert.assertEquals("value2", entry.getLabels().get("label2"));

        // test that no space after the comma still works
        entry = FileSdConfig.Entry.buildFromString("host:1234{label1=value1, label2=value2,l3=v3}");
        Assert.assertEquals(1, entry.getTargets().size());
        Assert.assertEquals("host:1234", entry.getTargets().get(0));
        Assert.assertEquals(3, entry.getLabels().size());
        Assert.assertEquals("value1", entry.getLabels().get("label1"));
        Assert.assertEquals("value2", entry.getLabels().get("label2"));
        Assert.assertEquals("v3", entry.getLabels().get("l3"));

        try {
            FileSdConfig.Entry.buildFromString("host:1234{}");
            Assert.fail("Cannot specify empty labels - should have failed");
        } catch (Exception expected) {
        }

        try {
            FileSdConfig.Entry.buildFromString("host:1234{label1}");
            Assert.fail("Cannot specify empty label value - should have failed");
        } catch (Exception expected) {
        }

        try {
            FileSdConfig.Entry.buildFromString("host:1234{=label1}");
            Assert.fail("Cannot specify empty label name - should have failed");
        } catch (Exception expected) {
        }

    }

    @Test
    public void testEmptyJson() throws Exception {
        FileSdConfig config = new FileSdConfig();
        String json = config.toJson();
        Assert.assertEquals("[]", json);
    }

    @Test
    public void testOneTargetJson() throws Exception {
        FileSdConfig.Entry entry1 = new FileSdConfig.Entry();
        entry1.addTarget("host:1234");

        FileSdConfig config = new FileSdConfig();
        config.addEntry(entry1);

        String json = config.toJson();
        Assert.assertEquals("[{\"targets\":[\"host:1234\"],\"labels\":{}}]", json);
    }

    @Test
    public void testOneTargetOneLabelJson() throws Exception {
        FileSdConfig.Entry entry1 = new FileSdConfig.Entry();
        entry1.addTarget("host:1234");
        entry1.addLabel("label1", "value1");

        FileSdConfig config = new FileSdConfig();
        config.addEntry(entry1);

        String json = config.toJson();
        Assert.assertEquals("[{\"targets\":[\"host:1234\"],\"labels\":{\"label1\":\"value1\"}}]", json);
    }

    @Test
    public void testComplexJson() throws Exception {
        FileSdConfig.Entry entry1 = new FileSdConfig.Entry();
        entry1.addTarget("host:1234");
        entry1.addTarget("foo:4567");
        entry1.addLabel("label1", "value1");
        entry1.addLabel("label2", "value2");

        FileSdConfig.Entry entry2 = new FileSdConfig.Entry();
        entry2.addTarget("anotherhost:4321");
        entry2.addTarget("anotherfoo:7654");
        entry2.addLabel("anotherlabel1", "anothervalue1");
        entry2.addLabel("anotherlabel2", "anothervalue2");

        FileSdConfig config = new FileSdConfig();
        config.addEntry(entry1);
        config.addEntry(entry2);

        String json = config.toJson();
        Assert.assertEquals(""
                + "["
                + "{\"targets\":[\"host:1234\",\"foo:4567\"],"
                + "\"labels\":{\"label1\":\"value1\",\"label2\":\"value2\"}},"
                + "{\"targets\":[\"anotherhost:4321\",\"anotherfoo:7654\"],"
                + "\"labels\":{\"anotherlabel2\":\"anothervalue2\",\"anotherlabel1\":\"anothervalue1\"}}"
                + "]",
                json);
    }
}
