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
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import javax.ws.rs.core.StreamingOutput;

import org.hawkular.commons.doc.DocEndpoint;
import org.hawkular.commons.doc.DocParameter;
import org.hawkular.commons.doc.DocParameters;
import org.hawkular.commons.doc.DocPath;
import org.hawkular.commons.doc.DocResponse;
import org.hawkular.commons.doc.DocResponses;
import org.hawkular.commons.json.JsonUtil;
import org.hawkular.inventory.api.InventoryService;
import org.hawkular.inventory.api.ResourceFilter;
import org.hawkular.inventory.api.model.Inventory;
import org.hawkular.inventory.api.model.InventoryHealth;
import org.hawkular.inventory.api.model.MetricsEndpoint;
import org.hawkular.inventory.api.model.Resource;
import org.hawkular.inventory.api.model.ResourceNode;
import org.hawkular.inventory.api.model.ResourceType;
import org.hawkular.inventory.api.model.ResultSet;
import org.hawkular.inventory.handlers.ResponseUtil.ApiError;
import org.hawkular.inventory.log.InventoryLoggers;
import org.hawkular.inventory.log.MsgLogger;
import org.jboss.resteasy.core.Dispatcher;
import org.jboss.resteasy.core.ResourceMethodRegistry;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@Path("/")
@DocEndpoint(value = "/", description = "Inventory Handlers")
public class InventoryHandlers {

    private static final MsgLogger log = InventoryLoggers.getLogger(InventoryHandlers.class);

    private ManifestUtil manifestUtil = new ManifestUtil();

    @EJB
    private InventoryService inventoryService;

    /*
        Let's order the methods by their Path
     */

    @GET
    @Path("/")
    @Produces("application/json; qs=0.8")
    public Response listRestPaths(@Context Dispatcher dispatcher) {
        try {
            List<RESTPathDiscovery.Path> discovered = RESTPathDiscovery.discover((ResourceMethodRegistry) dispatcher.getRegistry());
            String json = JsonUtil.getMapper()
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(discovered);
            return ResponseUtil.ok(json);
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @GET
    @Path("/")
    @Produces(TEXT_HTML)
    public Response listRestPathsAsHtml(@Context Dispatcher dispatcher) {
        List<RESTPathDiscovery.Path> discovered = RESTPathDiscovery.discover((ResourceMethodRegistry) dispatcher.getRegistry());
        StringBuilder sb = new StringBuilder();
        sb.append("<h1>Hawkular Inventory - REST API overview</h1>")
                // TODO: doc url
                .append("This is a generated list of available endpoints. Check the documentation for more details.");
        discovered.forEach(r -> {
            sb.append("<h2>").append(r.getPath()).append("</h2><ul>");
            r.getMethods().forEach(rm -> {
                sb.append("<li>").append(rm.getVerb()).append(" ");
                if (rm.getConsuming() != null) {
                    sb.append("consumes <i>").append(rm.getConsuming()).append("</i> ");
                }
                if (rm.getProducing() != null) {
                    sb.append("produces <i>").append(rm.getProducing()).append("</i> ");
                }
                sb.append("</li>");
            });
            sb.append("</ul>");
        });
        return Response.ok(sb.toString()).build();
    }

    @DocPath(method = "GET",
            path = "/export",
            name = "Export all resources and resource types.",
            notes = "This endpoint produces a streaming response.")
    @DocResponses(value = {
            @DocResponse(code = 200, message = "Success, inventory exported.", response = Inventory.class),
            @DocResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    @GET
    @Path("/export")
    @Produces(APPLICATION_JSON)
    public Response exportInventory() {
        try {
            StreamingOutput streamingOutput = inventoryService::buildExport;
            return ResponseUtil.ok(streamingOutput);
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @DocPath(method = "GET",
            path = "/get-inventory-config/{templateName}",
            name = "Get an existing inventory config file.",
            produces = TEXT_PLAIN)
    @DocParameters(value = {
            @DocParameter(name = "templateName", required = true, path = true,
                    description = "Inventory config file name to be retrieved.")
    })
    @DocResponses(value = {
            @DocResponse(code = 200, message = "Success, config file found.", response = String.class),
            @DocResponse(code = 404, message = "Config file not found.", response = ApiError.class),
            @DocResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
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

    @DocPath(method = "GET",
            path = "/get-jmx-exporter-config/{templateName}",
            name = "Get an existing jmx exporter config file.",
            produces = TEXT_PLAIN)
    @DocParameters(value = {
            @DocParameter(name = "templateName", required = true, path = true,
                    description = "Jmx exporter config file name to be retrieved.")
    })
    @DocResponses(value = {
            @DocResponse(code = 200, message = "Success, config file found.", response = String.class),
            @DocResponse(code = 404, message = "Config file not found.", response = ApiError.class),
            @DocResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
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

    @DocPath(method = "GET",
            path = "/health",
            name = "Get last health information collected.",
            notes = "Metrics collection task is performed asynchronously.")
    @DocResponses(value = {
            @DocResponse(code = 200, message = "Success.", response = InventoryHealth.class),
            @DocResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    @GET
    @Path("/health")
    @Produces(APPLICATION_JSON)
    public Response getInventoryHealth() {
        try {
            return ResponseUtil.ok(inventoryService.getHealthStatus());
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @DocPath(method = "POST",
            path = "/import",
            name = "Import a list of resources and resource types.",
            notes = "Previous resources and resource types stored under the same identifier will be overwritten.")
    @DocParameters(value = {
            @DocParameter(required = true, body = true, type = Inventory.class,
                    description = "The list of resources and resource types to be imported.")
    })
    @DocResponses(value = {
            @DocResponse(code = 200, message = "Success, inventory exported."),
            @DocResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    @POST
    @Path("/import")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response importInventory(final Inventory inventory) {
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

    @DocPath(method = "POST", path = "/register-metrics-endpoint", name = "Registers a feed metrics endpoint so it can be scraped for metric data.", notes = "This will write a configuration file for Prometheus so it can begin collecting metrics.")
    @DocParameters(value = {
            @DocParameter(required = true, body = true, type = MetricsEndpoint.class, description = "Describes the metrics endpoint t be registered.")
    })
    @DocResponses(value = {
            @DocResponse(code = 200, message = "Success, metrics endpoint has been registered."),
            @DocResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    @POST
    @Path("/register-metrics-endpoint")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    public Response registerMetricsEndpoint(final MetricsEndpoint metricsEndpoint) {
        try {
            boolean ok;
            if (metricsEndpoint != null) {
                ok = inventoryService.registerMetricsEndpoint(metricsEndpoint);
            } else {
                log.errorNullValue("metricsEndpoint");
                ok = false;
            }

            if (ok) {
                return ResponseUtil.ok();
            } else {
                return ResponseUtil.internalError("Cannot register metrics endpoint. Server-side logs has details.");
            }
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @DocPath(method = "GET",
            path = "/resources",
            name = "Get resources with optional filtering.",
            notes = "If not filtering defined it fetches all resources with default pagination.")
    @DocParameters(value = {
            @DocParameter(name = "root", type = Boolean.class,
                    description = "If true returns only top level resources. Default value is 'false'."),
            @DocParameter(name = "feedId",
                    description = "Filter resources by feedId"),
            @DocParameter(name = "typeId",
                    description = "Filter resources by typeId"),
            @DocParameter(name = "starOffSet", type = Long.class,
                    description = "Return results starting from an specific offset. Default value is 0."),
            @DocParameter(name = "maxResults", type = Integer.class,
                    description = "Define the maximum number of results on this query. Default value is 100.")
    })
    @DocResponses(value = {
            @DocResponse(code = 200, message = "Successfully fetched list of resources.", response = ResultSet.class),
            @DocResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    @GET
    @Path("/resources")
    @Produces(APPLICATION_JSON)
    public Response getResources(@QueryParam("root") @DefaultValue("false") final boolean root,
                                 @QueryParam("feedId") final String feedId,
                                 @QueryParam("typeId") final String typeId,
                                 @QueryParam("startOffSet") @DefaultValue("0") final Long startOffset,
                                 @QueryParam("maxResults") @DefaultValue("100") final Integer maxResults) {
        try {
            return ResponseUtil.ok(inventoryService.getResources(new ResourceFilter(root, feedId, typeId), startOffset, maxResults));
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @DocPath(method = "GET",
            path = "/resources/{id}",
            name = "Get a resource from its identifier.")
    @DocParameters(value = {
            @DocParameter(name = "id", path = true,
                    description = "Resource identifier.")
    })
    @DocResponses(value = {
            @DocResponse(code = 200, message = "Success, resource found.", response = Resource.class),
            @DocResponse(code = 404, message = "Resource not found.", response = ApiError.class),
            @DocResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
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

    @DocPath(method = "DELETE",
            path = "/resources",
            name = "Delete resources.",
            notes = "A comma list of resource IDs can be used as web parameter. + \n" +
                    "WARNING: If not IDs list is provided ALL resources will be deleted.")
    @DocParameters(value = {
            @DocParameter(name = "ids",
                    description = "Comma list of Resource identifiers to delete.")
    })
    @DocResponses(value = {
            @DocResponse(code = 200, message = "Success, resources deleted."),
            @DocResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    @DELETE
    @Path("/resources")
    @Produces(APPLICATION_JSON)
    public Response deleteResources(@QueryParam("ids") List<String> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                inventoryService.deleteAllResources();
            } else {
                inventoryService.deleteResources(ids);
            }
            return ResponseUtil.ok();
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @DocPath(method = "DELETE",
            path = "/resources/{id}",
            name = "Delete a resource from its identifier.")
    @DocParameters(value = {
            @DocParameter(name = "id",
                    description = "Resource identifier.")
    })
    @DocResponses(value = {
            @DocResponse(code = 200, message = "Success, resource deleted."),
            @DocResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    @DELETE
    @Path("/resources/{id}")
    @Produces(APPLICATION_JSON)
    public Response deleteResource(@PathParam("id") final String id) {
        try {
            inventoryService.deleteResources(Collections.singleton(id));
            return ResponseUtil.ok();
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @DocPath(method = "GET",
            path = "/resources/{id}/tree",
            name = "Get a complete resource tree from its identifier.")
    @DocParameters(value = {
            @DocParameter(name = "id", path = true,
                    description = "Resource identifier.")
    })
    @DocResponses(value = {
            @DocResponse(code = 200, message = "Success, resource found.", response = ResourceNode.class),
            @DocResponse(code = 404, message = "Resource not found.", response = ApiError.class),
            @DocResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
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

    @DocPath(method = "GET",
            path = "/resources/{id}/children",
            name = "Get children from a resource from its identifier.",
            notes = "If not filtering defined it fetches all children resources with default pagination.")
    @DocParameters(value = {
            @DocParameter(name = "starOffSet", type = Long.class,
                    description = "Return results starting from an specific offset. Default value is 0."),
            @DocParameter(name = "maxResults", type = Integer.class,
                    description = "Define the maximum number of results on this query. Default value is 100.")
    })
    @DocResponses(value = {
            @DocResponse(code = 200, message = "Successfully fetched list of resources.", response = ResultSet.class),
            @DocResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    @GET
    @Path("/resources/{id}/children")
    @Produces(APPLICATION_JSON)
    public Response getChildren(@PathParam("id") final String id,
                                @DefaultValue("0") @QueryParam("startOffSet") final Long startOffset,
                                @DefaultValue("100") @QueryParam("maxResults") final Integer maxResults) {
        try {
            return ResponseUtil.ok(inventoryService.getChildren(id, startOffset, maxResults));
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @DocPath(method = "GET",
            path = "/resources/{id}/parent",
            name = "Get the parent of a resource from its identifier.")
    @DocResponses(value = {
            @DocResponse(code = 200, message = "Successfully fetched parent resource.", response = Resource.class),
            @DocResponse(code = 204, message = "No parent, resource is root.", response = Resource.class),
            @DocResponse(code = 404, message = "Resource not found.", response = ApiError.class),
            @DocResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    @GET
    @Path("/resources/{id}/parent")
    @Produces(APPLICATION_JSON)
    public Response getParent(@PathParam("id") final String id) {
        try {
            Optional<Resource> optR = inventoryService.getResourceById(id);
            if (!optR.isPresent()) {
                return ResponseUtil.notFound("Resource id [" + id + "] not found");
            }
            Resource r = optR.get();
            if (r.getParentId() == null) {
                return Response.status(Response.Status.NO_CONTENT).build();
            }
            return inventoryService.getResourceById(r.getParentId())
                    .map(ResponseUtil::ok)
                    .orElseGet(() -> ResponseUtil.notFound("Graph inconsistency detected." +
                            " Parent id [" + r.getParentId() + "] not found"));
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @DocPath(method = "GET",
            path = "/types",
            name = "Get resource types.",
            notes =  "If not filtering defined it fetches all resource types with default pagination.")
    @DocParameters(value = {
            @DocParameter(name = "starOffSet", type = Long.class,
                    description = "Return results starting from an specific offset. Default value is 0."),
            @DocParameter(name = "maxResults", type = Integer.class,
                    description = "Define the maximum number of results on this query. Default value is 100.")
    })
    @DocResponses(value = {
            @DocResponse(code = 200, message = "Successfully fetched list of resources types.", response = ResultSet.class),
            @DocResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
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

    @DocPath(method = "DELETE",
            path = "/types",
            name = "Delete resource types.",
            notes = "A comma list of resource type IDs can be used as web parameter. + \n" +
                    "WARNING: If not resource type IDs list is provided ALL resource types will be deleted.")
    @DocParameters(value = {
            @DocParameter(name = "typeIds",
                    description = "Comma list of resource type identifiers to delete.")
    })
    @DocResponses(value = {
            @DocResponse(code = 200, message = "Success, resource types deleted."),
            @DocResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    @DELETE
    @Path("/types")
    @Produces(APPLICATION_JSON)
    public Response deleteTypes(@QueryParam("typeIds") List<String> typeIds) {
        try {
            if (typeIds == null || typeIds.isEmpty()) {
                inventoryService.deleteAllTypes();
            } else {
                inventoryService.deleteResourceTypes(typeIds);
            }
            return ResponseUtil.ok();
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @DocPath(method = "DELETE",
            path = "/type/{typeId}",
            name = "Delete resource type from its identifier.")
    @DocParameters(value = {
            @DocParameter(name = "typeId",
                    description = "Resource type identifier to delete.")
    })
    @DocResponses(value = {
            @DocResponse(code = 200, message = "Success, resource type deleted."),
            @DocResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    @DELETE
    @Path("/types/{typeId}")
    @Produces(APPLICATION_JSON)
    public Response deleteResourceType(@PathParam("typeId") final String typeId) {
        try {
            inventoryService.deleteResourceTypes(Collections.singleton(typeId));
            return ResponseUtil.ok();
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @DocPath(method = "GET",
            path = "/types/{typeId}",
            name = "Get a resource type from its identifier.")
    @DocParameters(value = {
            @DocParameter(name = "typeId", path = true,
                    description = "Resource type identifier.")
    })
    @DocResponses(value = {
            @DocResponse(code = 200, message = "Success, resource type found.", response = ResourceType.class),
            @DocResponse(code = 404, message = "Resource not found.", response = ApiError.class),
            @DocResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    @GET
    @Path("/types/{typeId}")
    @Produces(APPLICATION_JSON)
    public Response getResourceType(@PathParam("typeId") final String typeId) {
        try {
            return inventoryService.getResourceType(typeId)
                    .map(ResponseUtil::ok)
                    .orElseGet(() -> ResponseUtil.notFound("Resource type [" + typeId + "] not found"));
        } catch (Exception e) {
            return ResponseUtil.internalError(e);
        }
    }

    @DocPath(method = "GET",
            path = "/status",
            name = "Get status info.",
            notes = "Status fields: + \n" +
                    "``` \n" +
                    "{\n" +
                    "    \"status\":\"<UP>|<DOWN>\", \n" +
                    "    \"Implementation-Version\":\"<Version>\", \n" +
                    "    \"Built-From-Git-SHA1\":\"<Git-SHA1>\" \n" +
                    "}\n" +
                    "``` \n")
    @DocResponses(value = {
            @DocResponse(code = 200, message = "Success.", response = DocResponse.OBJECT.class),
            @DocResponse(code = 500, message = "Internal server error.", response = ApiError.class)
    })
    @GET
    @Path("/status")
    @Produces(APPLICATION_JSON)
    public Response status(@Context ServletContext servletContext) {
        final Map<String, String> status = new HashMap<>();
        status.putAll(manifestUtil.getFrom());
        if (inventoryService.isRunning()) {
            status.put("status", "UP");
            return Response.ok(status).build();
        } else {
            status.put("status", "DOWN");
            return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(status).build();
        }
    }
}
