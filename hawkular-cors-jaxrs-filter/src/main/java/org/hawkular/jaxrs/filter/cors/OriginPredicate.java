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
package org.hawkular.jaxrs.filter.cors;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Stefan Negrea
 * @author Joel Takvorian
 */
public class OriginPredicate implements Predicate<String> {

    private final boolean allowAnyOrigin;
    private final Set<URI> allowedOrigins;

    public OriginPredicate(String allowedCorsOriginsConfig) {
        if (allowedCorsOriginsConfig == null || allowedCorsOriginsConfig.isEmpty()) {
            allowedOrigins = Collections.emptySet();
            allowAnyOrigin = false;
            return;
        }

        if (Headers.ALLOW_ALL_ORIGIN.equals(allowedCorsOriginsConfig.trim())) {
            allowAnyOrigin = true;
            allowedOrigins = null;
        } else {
            allowAnyOrigin = false;
            Set<URI> parsedOrigins = Arrays.stream(allowedCorsOriginsConfig.split(","))
                    .map(String::trim)
                    .map(URI::create)
                    .collect(Collectors.toSet());
            allowedOrigins = Collections.unmodifiableSet(parsedOrigins);
        }
    }

    /**
     * Check whether requests from the specified origin must be allowed.
     * @param requestOrigin origin as reported by the client, {@code null} if unknown.
     *
     * @return {@code true} if the origin is allowed, else {@code false}.
     */
    @Override public boolean test(String requestOrigin) {
        if (allowAnyOrigin) {
            return true;
        }

        if (requestOrigin == null || allowedOrigins.isEmpty()) {
            return false;
        }

        URI requestOriginURI;
        try {
            requestOriginURI = new URI(requestOrigin);
        } catch (URISyntaxException e) {
            return false;
        }

        String requestScheme = requestOriginURI.getScheme();
        String requestHost = requestOriginURI.getHost();
        int requestPort = requestOriginURI.getPort();

        if (requestScheme == null || requestHost == null) {
            return false;
        }

        for (URI allowedOrigin : allowedOrigins) {
            if ((requestHost.equalsIgnoreCase(allowedOrigin.getHost())
                    || requestHost.toLowerCase().endsWith("." + allowedOrigin.getHost().toLowerCase()))
                    && requestPort == allowedOrigin.getPort()
                    && requestScheme.equalsIgnoreCase(allowedOrigin.getScheme())) {
                return true;
            }
        }

        return false;
    }
}
