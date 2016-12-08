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

import java.util.function.Predicate;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * @author Stefan Negrea
 * @author Joel Takvorian
 */
public final class CorsFilters {

    private CorsFilters() {
    }

    /**
     * Apply CORS filter on request
     * @param requestContext request context
     * @param predicate must return {@code true} if the input origin is allowed, else {@code false}.
     */
    public static void filterRequest(ContainerRequestContext requestContext, Predicate<String> predicate) {
        //NOT a CORS request
        String requestOrigin = requestContext.getHeaderString(Headers.ORIGIN);
        if (requestOrigin == null) {
            return;
        }

        if (!predicate.test(requestOrigin)) {
            requestContext.abortWith(Response.status(Response.Status.BAD_REQUEST).build());
            return;
        }

        //It is a CORS pre-flight request, there is no route for it, just return 200
        if (requestContext.getMethod().equalsIgnoreCase(HttpMethod.OPTIONS)) {
            requestContext.abortWith(Response.status(Response.Status.OK).build());
        }
    }

    /**
     * Apply CORS headers on response
     * @param requestContext request context
     * @param responseContext response context
     * @param extraAccesControlAllowHeaders eventual extra allowed headers (nullable)
     */
    public static void filterResponse(ContainerRequestContext requestContext,
                                      ContainerResponseContext responseContext,
                                      String extraAccesControlAllowHeaders) {

        String requestOrigin = requestContext.getHeaderString(Headers.ORIGIN);
        if (requestOrigin == null) {
            return;
        }

        // CORS validation already checked on request filter, see AbstractCorsRequestFilter
        MultivaluedMap<String, Object> responseHeaders = responseContext.getHeaders();
        responseHeaders.add(Headers.ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);
        responseHeaders.add(Headers.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        responseHeaders.add(Headers.ACCESS_CONTROL_ALLOW_METHODS,
                Headers.DEFAULT_CORS_ACCESS_CONTROL_ALLOW_METHODS);
        responseHeaders.add(Headers.ACCESS_CONTROL_MAX_AGE, 72 * 60 * 60);

        if (extraAccesControlAllowHeaders != null) {
            responseHeaders.add(Headers.ACCESS_CONTROL_ALLOW_HEADERS,
                    Headers.DEFAULT_CORS_ACCESS_CONTROL_ALLOW_HEADERS + ","
                            + extraAccesControlAllowHeaders.trim());
        } else {
            responseHeaders.add(Headers.ACCESS_CONTROL_ALLOW_HEADERS,
                    Headers.DEFAULT_CORS_ACCESS_CONTROL_ALLOW_HEADERS);
        }
    }
}
