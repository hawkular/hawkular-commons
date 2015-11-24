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
import java.io.InputStream;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.servlet.ServletContext;

/**
 * Manifest extraction. Credits to Hawkular Metrics team.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
class ManifestUtil {
    static final String IMPLEMENTATION_VERSION = "Implementation-Version";
    static final String BUILT_FROM_GIT = "Built-From-Git-SHA1";

    private static final String[] VERSION_ATTRIBUTES = new String[] { IMPLEMENTATION_VERSION,
            BUILT_FROM_GIT };
    static final String UNKNOWN_VALUE = "Unknown";

    /**
     * Returns a {@link Map} with keys being the elements from {@link #VERSION_ATTRIBUTES} and values being the
     * respective values from the manifest as loaded using the given {@code servletContext}. Some or all values may be
     * missing from the result map.
     *
     * @param servletContext the servlet context to load the manifest values from
     * @return a {@link Map} of attributes
     * @throws IOException on problems with reading {@code /META-INF/MANIFEST.MF} file from the {@code servletContext}
     */
    public static Map<String, String> getVersionAttributes(ServletContext servletContext) throws IOException {
        URL url = servletContext.getResource("/META-INF/MANIFEST.MF");
        if (url != null) {
            return getVersionAttributes(url);
        } else {
            return new LinkedHashMap<>();
        }
    }

    /**
     * For the sake of unit testing
     *
     * @param url the URL to load the MANIFEST.MF file from
     * @return a {@link Map} of attributes
     * @throws IOException on problems with reading {@code /META-INF/MANIFEST.MF} file from the given {@code url}
     */
    static Map<String, String> getVersionAttributes(URL url) throws IOException {
        Map<String, String> ret = new LinkedHashMap<>();
        try (InputStream inputStream = url.openStream()) {
            Manifest manifest = new Manifest(inputStream);
            Attributes attributes = manifest.getMainAttributes();
            for (String key : VERSION_ATTRIBUTES) {
                final String value = attributes.getValue(key);
                ret.put(key, value == null ? UNKNOWN_VALUE : value);
            }
        }
        return ret;
    }

}
