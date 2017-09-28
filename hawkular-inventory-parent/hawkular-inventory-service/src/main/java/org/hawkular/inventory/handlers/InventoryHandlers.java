/*
 * Copyright 2014-2017 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.inventory.handlers;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJB;
import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.hawkular.inventory.api.Import;
import org.hawkular.inventory.api.InventoryService;
import org.hawkular.inventory.log.InventoryLoggers;
import org.hawkular.inventory.log.MsgLogger;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@Path("/")
public class InventoryHandlers {

    private static final MsgLogger log = InventoryLoggers.getLogger(InventoryHandlers.class);

    @EJB
    private InventoryService inventoryService;

    /*
        Let's order the methods by their Path
     */

    @GET
    @Path("/get-inventory-config/{templateName}")
    @Produces(TEXT_PLAIN)
    public Response getAgentConfig(@PathParam("templateName") final String templateName) {
        try {
            return inventoryService.getAgentConfig(templateName)
                    .map(ResponseUtil::ok)
                    .orElseGet(() -> ResponseUtil.notFound("Inventory config [" + templateName + "] not found"));
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @GET
    @Path("/get-jmx-exporter-config/{templateName}")
    @Produces(TEXT_PLAIN)
    public Response getJMXExporterConfig(@PathParam("templateName") final String templateName) {
        try {
            return inventoryService.getJMXExporterConfig(templateName)
                    .map(ResponseUtil::ok)
                    .orElseGet(() -> ResponseUtil.notFound("JMX Exporter config [" + templateName + "] not found"));
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @POST
    @Path("/import")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response importInventory(final Import inventory) {
        try {
            if (inventory != null) {
                inventoryService.addResource(inventory.getResources());
                inventoryService.addResourceType(inventory.getTypes());
            }
            return ResponseUtil.ok();
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @GET
    @Path("/resources")
    @Produces(APPLICATION_JSON)
    public Response getResources(@QueryParam("root") @DefaultValue("false") final boolean root,
                                 @QueryParam("typeId") final String typeId,
                                 @QueryParam("startOffSet") @DefaultValue("0") final Long startOffset,
                                 @QueryParam("maxResults") @DefaultValue("100") final Integer maxResults) {
        try {
            return ResponseUtil.ok(inventoryService.getResources(root, typeId, startOffset, maxResults));
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @GET
    @Path("/resources/{id}")
    @Produces(APPLICATION_JSON)
    public Response getResourceById(@PathParam("id") final String id) {
        try {
            return inventoryService.getResourceById(id)
                    .map(ResponseUtil::ok)
                    .orElseGet(() -> ResponseUtil.notFound("Resource id [" + id + "] not found"));
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @DELETE
    @Path("/resources/{id}")
    @Produces(APPLICATION_JSON)
    public Response deleteResource(@PathParam("id") final String id) {
        try {
            inventoryService.deleteResource(id);
            return ResponseUtil.ok();
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @GET
    @Path("/resources/{id}/tree")
    @Produces(APPLICATION_JSON)
    public Response getTree(@PathParam("id") final String id) {
        try {
            return inventoryService.getTree(id)
                    .map(ResponseUtil::ok)
                    .orElseGet(() -> ResponseUtil.notFound("Resource id [" + id + "] not found"));
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @GET
    @Path("/types")
    @Produces(APPLICATION_JSON)
    public Response getAllResourceTypes(@DefaultValue("0") @QueryParam("startOffSet") final Long startOffset,
                                        @DefaultValue("100") @QueryParam("maxResults") final Integer maxResults) {
        try {
            return ResponseUtil.ok(inventoryService.getResourceTypes(startOffset, maxResults));
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @DELETE
    @Path("/type/{typeId}")
    @Produces(APPLICATION_JSON)
    public Response deleteResourceType(@PathParam("typeId") final String typeId) {
        try {
            inventoryService.deleteResourceType(typeId);
            return ResponseUtil.ok();
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @GET
    @Path("/status")
    @Produces(APPLICATION_JSON)
    public Response status(@Context ServletContext servletContext) {
        final Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        return Response.ok(status).build();
    }
}
