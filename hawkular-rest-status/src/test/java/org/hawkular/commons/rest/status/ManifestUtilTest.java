/*
 * Copyright 2014-2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.commons.rest.status;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 */
public class ManifestUtilTest {

    @Test
    public void testEmpty() throws IOException {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put(ManifestUtil.IMPLEMENTATION_VERSION, ManifestUtil.UNKNOWN_VALUE);
        expected.put(ManifestUtil.BUILT_FROM_GIT, ManifestUtil.UNKNOWN_VALUE);
        Assert.assertEquals(expected,
                ManifestUtil.getVersionAttributes(getClass().getResource("/manifests/MANIFEST-empty.txt")));
    }

    @Test
    public void testFull() throws IOException {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put(ManifestUtil.IMPLEMENTATION_VERSION, "1.2.3.Final");
        expected.put(ManifestUtil.BUILT_FROM_GIT, "eb699f45f5461bb4cf52aca651679edb01802c04");
        Assert.assertEquals(expected,
                ManifestUtil.getVersionAttributes(getClass().getResource("/manifests/MANIFEST-full.txt")));
    }

    @Test
    public void testPartial() throws IOException {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put(ManifestUtil.IMPLEMENTATION_VERSION, ManifestUtil.UNKNOWN_VALUE);
        expected.put(ManifestUtil.BUILT_FROM_GIT, "eb699f45f5461bb4cf52aca651679edb01802c04");
        Assert.assertEquals(expected,
                ManifestUtil.getVersionAttributes(getClass().getResource("/manifests/MANIFEST-partial.txt")));
    }
}
