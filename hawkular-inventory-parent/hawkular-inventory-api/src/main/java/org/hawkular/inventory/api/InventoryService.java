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
    void addResourceType(ResourceType rt);
    void deleteResource(String id);
    void deleteResourceType(String type);
    Optional<Resource> getResourceById(String id);
    Optional<ResourceNode> getTree(String parentId);
    Collection<Resource> getAllTopResources();
    Collection<ResourceType> getAllResourceTypes();
    Collection<Resource> getResourcesByType(String typeId);
    Optional<ResourceType> getResourceType(String typeId);
    Optional<String> getAgentConfig(String templateName);
    Optional<String> getJMXExporterConfig(String templateName);
}
