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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.wordnik.swagger.annotations.Api;

/**
 * REST endpoint for status.
 *
 * <p>This handles the {@code /status} URL and outputs information read from the META-INF/MANIFEST.MF file together
 * with whatever information injected into it by maps of strings qualified by {@link RestStatusInfo @RestStatusInfo}.
 * There can be any number of such maps (including none) and all of them will be included in the output.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 * @author Lukas Krejci
 */
@Path("/status")
@Api(value = "/status", description = "Status of the component service.")
@ApplicationScoped
public class RestStatusHandler {
    @Inject @RestStatusInfo
    private Instance<Map<String, String>> details;

    private Map<String, String> status;

    @GET
    @Path("/")
    @Produces(APPLICATION_JSON)
    public Response status(@Context ServletContext servletContext) {
        Map<String, String> status = getStatus(servletContext);
        if (!details.isUnsatisfied()) {
            //include stuff from all the status info producers found
            for (Map<String, String> details: this.details) {
                status.putAll(details);
            }
        }
        return Response.ok(status).build();
    }

    private Map<String, String> getStatus(ServletContext servletContext) {
        if (status == null) {
            status = new HashMap<>(ManifestUtil.getFrom(servletContext));
        }

        return status;
    }

}
