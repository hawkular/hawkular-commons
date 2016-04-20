/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.inventory.paths;

import java.io.IOException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 */
@RunWith(Arquillian.class)
public class CanonicalPathITest {
    private static final Logger log = Logger.getLogger(CanonicalPathITest.class);

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, CanonicalPathITest.class.getSimpleName() + ".war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(
                        CanonicalPathITest.class
                                .getResource("/hawkular-inventory-paths/jboss-deployment-structure.xml"),
                        "jboss-deployment-structure.xml");
        // ZipExporter exporter = new ZipExporterImpl(archive);
        // exporter.exportTo(new File("target", CanonicalPathITest.class.getSimpleName() + ".war"));
        return archive;
    }

    @Test
    public void testCanonicalPath() throws IOException, InterruptedException {
        CanonicalPath p = CanonicalPath.fromString("/t;t/f;f/r;r");
        checkPath(p, SegmentType.t, "t", SegmentType.f, "f", SegmentType.r, "r");
    }

    private void checkPath(CanonicalPath path, Object... pathSpec) {
        Assert.assertEquals(pathSpec.length / 2, path.getPath().size());
        for (int i = 0; i < pathSpec.length; i += 2) {
            SegmentType t = (SegmentType) pathSpec[i];
            String id = (String) pathSpec[i + 1];

            CanonicalPath.Segment s = path.getPath().get(i / 2);

            //noinspection AssertEqualsBetweenInconvertibleTypes
            Assert.assertEquals(t, s.getElementType());
            Assert.assertEquals(id, s.getElementId());
        }
    }
}
