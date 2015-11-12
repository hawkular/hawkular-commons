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

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.servlet.ServletContext;

/**
 * Manifest extraction.
 * Credits to Hawkular Metrics team.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 * @since 0.2.4.Final
 */
class ManifestUtil {
    private static final String IMPLEMENTATION_VERSION = "Implementation-Version";
    private static final String BUILT_FROM_GIT = "Built-From-Git-SHA1";

    private static final String[] VERSION_ATTRIBUTES = new String[]{IMPLEMENTATION_VERSION,
            BUILT_FROM_GIT};

    public static Map<String, String> getFrom(ServletContext servletContext) {
        Map<String, String> ret = new HashMap<>();
        try (InputStream inputStream = servletContext.getResourceAsStream("/META-INF/MANIFEST.MF")) {
            Manifest manifest = new Manifest(inputStream);
            Attributes attr = manifest.getMainAttributes();
            for (String attribute : VERSION_ATTRIBUTES) {
                ret.put(attribute, attr.getValue(attribute));
            }
        } catch (Exception e) {
            for (String attribute : VERSION_ATTRIBUTES) {
                if (ret.get(attribute) == null) {
                    ret.put(attribute, "Unknown");
                }
            }
        }
        return ret;
    }
}
