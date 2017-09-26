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
package org.hawkular.inventory.api;

import java.util.Collection;
import java.util.Optional;

import org.hawkular.inventory.model.Resource;
import org.hawkular.inventory.model.ResourceType;

/**
 * Provides access to the inventory backend
 * @author Joel Takvorian
 */
public interface InventoryService {
    /**
     * Add or replace a single resource
     */
    void addResource(Resource r);

    /**
     * Add or replace multiple resources
     */
    void addResource(Collection<Resource> r);

    /**
     * Add or replace a single resource type
     */
    void addResourceType(ResourceType rt);

    /**
     * Add or replace multiple resource types
     */
    void addResourceType(Collection<ResourceType> rt);

    /**
     * Delete a single resource by ID
     */
    void deleteResource(String id);

    /**
     * Delete a single resource type
     */
    void deleteResourceType(String type);

    /**
     * Get a resource from its ID. The resulting object includes {@link ResourceType}
     * @return the {@link ResourceWithType} if found
     */
    Optional<ResourceWithType> getResourceById(String id);

    /**
     * Get a resource from its ID, and load all its child subtree. The resulting objects include {@link ResourceType}
     * @return the {@link ResourceNode} if found
     */
    Optional<ResourceNode> getTree(String parentId);

    /**
     * Get top resources (aka. root resources) with the default pagination options (first 100 results)
     * @return top resources embedded in page object {@link ResultSet}
     */
    ResultSet<ResourceWithType> getTopResources();

    /**
     * Get top resources (aka. root resources) with the provided pagination options
     * @return top resources embedded in page object {@link ResultSet}
     */
    ResultSet<ResourceWithType> getTopResources(long startOffset, int maxResults);

    /**
     * Get resource types with the default pagination options (first 100 results)
     * @return resource types embedded in page object {@link ResultSet}
     */
    ResultSet<ResourceType> getResourceTypes();

    /**
     * Get resource types with the provided pagination options
     * @return resource types embedded in page object {@link ResultSet}
     */
    ResultSet<ResourceType> getResourceTypes(long startOffset, int maxResults);

    /**
     * Get resources of a given type, with the default pagination options (first 100 results)
     * @return list of resources, embedded in page object {@link ResultSet}
     */
    ResultSet<ResourceWithType> getResourcesByType(String typeId);

    /**
     * Get resources of a given type, with the provided pagination options
     * @return list of resources, embedded in page object {@link ResultSet}
     */
    ResultSet<ResourceWithType> getResourcesByType(String typeId, long startOffset, int maxResults);

    /**
     * Get resource type by ID
     * @return the {@link ResourceType} if found
     */
    Optional<ResourceType> getResourceType(String typeId);

    /**
     * Get the content of agents configuration file for a given template
     * @return config file content as String, if found
     */
    Optional<String> getAgentConfig(String templateName);

    /**
     * Get the content of JMX exporter configuration file for a given template
     * @return config file content as String, if found
     */
    Optional<String> getJMXExporterConfig(String templateName);
}
