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
 * @author Joel Takvorian
 */
public interface InventoryService {
    void addResource(Resource r);
    void addResource(Collection<Resource> r);
    void addResourceType(ResourceType rt);
    void addResourceType(Collection<ResourceType> rt);
    void deleteResource(String id);
    void deleteResourceType(String type);
    Optional<ResourceWithType> getResourceById(String id);
    Optional<ResourceNode> getTree(String parentId);
    ResultSet<ResourceWithType> getAllTopResources();
    ResultSet<ResourceWithType> getAllTopResources(long startOffset, int maxResults);
    ResultSet<ResourceType> getAllResourceTypes();
    ResultSet<ResourceType> getAllResourceTypes(long startOffset, int maxResults);
    ResultSet<ResourceWithType> getResourcesByType(String typeId);
    ResultSet<ResourceWithType> getResourcesByType(String typeId, long startOffset, int maxResults);
    Optional<ResourceType> getResourceType(String typeId);
    Optional<String> getAgentConfig(String templateName);
    Optional<String> getJMXExporterConfig(String templateName);
}
