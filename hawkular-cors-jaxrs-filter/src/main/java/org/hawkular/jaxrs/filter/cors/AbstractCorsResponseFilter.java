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

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

/**
 * @author Stefan Negrea
 */
public abstract class AbstractCorsResponseFilter implements ContainerResponseFilter {

    protected abstract boolean isAllowedOrigin(String requestOrigin);

    protected abstract String getExtraAccessControlAllowHeaders();

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {

        String requestOrigin = requestContext.getHeaderString(Headers.ORIGIN);
        if (requestOrigin == null) {
            return;
        }

        if (isAllowedOrigin(requestOrigin)) {
            MultivaluedMap<String, Object> responseHeaders = responseContext.getHeaders();
            responseHeaders.add(Headers.ACCESS_CONTROL_ALLOW_ORIGIN, requestOrigin);
            responseHeaders.add(Headers.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
            responseHeaders.add(Headers.ACCESS_CONTROL_ALLOW_METHODS,
                    Headers.DEFAULT_CORS_ACCESS_CONTROL_ALLOW_METHODS);
            responseHeaders.add(Headers.ACCESS_CONTROL_MAX_AGE, 72 * 60 * 60);

            String extraAccesControlAllowHeaders = getExtraAccessControlAllowHeaders();
            if (extraAccesControlAllowHeaders != null) {
                responseHeaders.add(Headers.ACCESS_CONTROL_ALLOW_HEADERS,
                        Headers.DEFAULT_CORS_ACCESS_CONTROL_ALLOW_HEADERS + ","
                                + extraAccesControlAllowHeaders.trim());
            } else {
                responseHeaders.add(Headers.ACCESS_CONTROL_ALLOW_HEADERS,
                        Headers.DEFAULT_CORS_ACCESS_CONTROL_ALLOW_HEADERS);
            }
        } else {
            responseContext.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
        }
    }
}
