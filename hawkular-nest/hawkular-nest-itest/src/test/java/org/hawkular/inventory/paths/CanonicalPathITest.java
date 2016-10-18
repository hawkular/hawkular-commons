/*
 * Copyright 2014-2016 Red Hat, Inc. and/or its affiliates
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
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 */
public class CanonicalPathITest extends Arquillian {
    private static final Logger log = Logger.getLogger(CanonicalPathITest.class);
    public static final String GROUP = "inventory-paths";

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

    @Test(groups = { GROUP })
    public void testCanonicalPath() throws IOException, InterruptedException {
        CanonicalPath p = CanonicalPath.fromString("/t;t/f;f/r;r");
        checkPath(p, SegmentType.t, "t", SegmentType.f, "f", SegmentType.r, "r");
    }

    private void checkPath(CanonicalPath path, Object... pathSpec) {
        Assert.assertEquals(path.getPath().size(), pathSpec.length / 2);
        for (int i = 0; i < pathSpec.length; i += 2) {
            SegmentType t = (SegmentType) pathSpec[i];
            String id = (String) pathSpec[i + 1];

            CanonicalPath.Segment s = path.getPath().get(i / 2);

            //noinspection AssertEqualsBetweenInconvertibleTypes
            Assert.assertEquals(s.getElementType(), t);
            Assert.assertEquals(s.getElementId(), id);
        }
    }
}
