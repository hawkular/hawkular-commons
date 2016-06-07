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
package org.hawkular.jaxrs.filter.tenant;

import java.io.IOException;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;

/**
 * @author Juraci Paixão Kröhling
 */
public class TenantFilter implements ContainerRequestFilter {
    private static final String TENANT_HEADER_NAME = "Hawkular-Tenant";

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String headerValue = requestContext.getHeaderString(TENANT_HEADER_NAME);
        if (null == headerValue || headerValue.isEmpty()) {
            String message = String.format("The HTTP header %s has to be provided.", TENANT_HEADER_NAME);
            Response response = Response.status(Response.Status.BAD_REQUEST).entity(message).build();
            requestContext.abortWith(response);
        }
    }
}
