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
package org.hawkular.inventory.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.hawkular.inventory.api.InventoryService;
import org.hawkular.inventory.api.ResourceNode;
import org.hawkular.inventory.api.ResultSet;
import org.hawkular.inventory.log.InventoryLoggers;
import org.hawkular.inventory.log.MsgLogger;
import org.hawkular.inventory.model.Resource;
import org.hawkular.inventory.model.ResourceType;
import org.infinispan.Cache;
import org.infinispan.query.Search;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;

/**
 * @author Joel Takvorian
 */
@Local(InventoryService.class)
@Stateless
public class InventoryServiceIspn implements InventoryService {

    private static final MsgLogger log = InventoryLoggers.getLogger(InventoryServiceIspn.class);

    // TODO [lponce] this should be configurable
    private static final int MAX_RESULTS = 100;

    private final Path configPath;

    @Inject
    @Inventory
    private Cache<String, Object> backend;

    @Inject
    @Inventory
    private QueryFactory queryFactory;

    public InventoryServiceIspn() {
        configPath = Paths.get(System.getProperty("jboss.server.config.dir"), "hawkular");
    }

    InventoryServiceIspn(Cache<String, Object> backend, String configPath) {
        this.backend = backend;
        queryFactory = Search.getQueryFactory(backend);
        this.configPath = Paths.get(configPath);
    }

    @Override
    public void addResource(Resource r) {
        addResource(Arrays.asList(r));
    }

    @Override
    public void addResource(Collection<Resource> resources) {
        if (isEmpty(resources)) {
            return;
        }
        Map<String, Resource> map = resources.stream()
                .parallel()
                .collect(Collectors.toMap(r -> IspnPK.pk(r), r -> r));
        backend.putAll(map);
    }

    @Override
    public void addResourceType(ResourceType rt) {
        addResourceType(Arrays.asList(rt));
    }

    @Override
    public void addResourceType(Collection<ResourceType> resourceTypes) {
        if (isEmpty(resourceTypes)) {
            return;
        }
        Map<String, ResourceType> map = resourceTypes.stream()
                .parallel()
                .collect(Collectors.toMap(rt -> IspnPK.pk(rt), rt -> rt));
        backend.putAll(map);
    }

    @Override
    public void deleteResource(String id) {
        if (isEmpty(id)) {
            throw new IllegalArgumentException("Id must be not null");
        }
        // FIXME: remove subtree?
        backend.remove(IspnPK.pkResource(id));
    }

    @Override
    public void deleteResourceType(String type) {
        if (isEmpty(type)) {
            throw new IllegalArgumentException("Type must be not null");
        }
        backend.remove(IspnPK.pkResourceType(type));
    }

    @Override
    public Optional<Resource> getResourceById(String id) {
        if (isEmpty(id)) {
            throw new IllegalArgumentException("Resource id must be not null");
        }
        return Optional.ofNullable((Resource) backend.get(IspnPK.pkResource(id)));
    }

    @Override
    public Optional<ResourceNode> getTree(String parentId) {
        // Optimisation, make sure eveything gets in cache; can be removed safely
        // resourcesByRoot.get(parentId);

        return getResourceById(parentId)
                .map(r -> ResourceNode.fromResource(r,
                        this::getNullableResourceType,
                        this::getNullableResource));
    }

    @Override
    public ResultSet<Resource> getAllTopResources(long startOffset, int maxResults) {
        Query query = queryFactory.from(Resource.class)
                .having("rootId").isNull()
                .maxResults(maxResults)
                .startOffset(startOffset)
                .build();
        return new ResultSet<>(query.list(), (long) query.getResultSize(), startOffset);
    }

    @Override
    public ResultSet<Resource> getAllTopResources() {
        return getAllTopResources(0, MAX_RESULTS);
    }

    @Override
    public ResultSet<ResourceType> getAllResourceTypes(long startOffset, int maxResults) {
        Query query = queryFactory.from(ResourceType.class)
                .maxResults(maxResults)
                .startOffset(startOffset)
                .build();
        return new ResultSet<>(query.list(), (long) query.getResultSize(), startOffset);
    }

    @Override
    public ResultSet<ResourceType> getAllResourceTypes() {
        return getAllResourceTypes(0, MAX_RESULTS);
    }

    @Override
    public ResultSet<Resource> getResourcesByType(String typeId, long startOffset, int maxResults) {
        Query query = queryFactory.from(Resource.class)
                .having("typeId").equal(typeId)
                .maxResults(maxResults)
                .startOffset(startOffset)
                .build();
        return new ResultSet<>(query.list(), (long) query.getResultSize(), startOffset);
    }

    @Override
    public ResultSet<Resource> getResourcesByType(String typeId) {
        return getResourcesByType(typeId, 0, MAX_RESULTS);
    }

    @Override
    public Optional<ResourceType> getResourceType(String typeId) {
        if (isEmpty(typeId)) {
            throw new IllegalArgumentException("ResourceType id must be not null");
        }
        return Optional.ofNullable((ResourceType) backend.get(IspnPK.pkResourceType(typeId)));
    }

    @Override
    public Optional<String> getAgentConfig(String templateName) {
        return getConfig(templateName + "-inventory.yml");
    }

    @Override
    public Optional<String> getJMXExporterConfig(String templateName) {
        return getConfig(templateName + "-jmx-exporter.yml");
    }

    private Optional<String> getConfig(String fileName) {
        // TODO: maybe some defensive check against file traversal attack?
        //  Or check that "resourceType" is in a whitelist of types?
        try {
            byte[] encoded = Files.readAllBytes(configPath.resolve(fileName));
            return Optional.of(new String(encoded, "UTF-8"));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private Resource getNullableResource(String id) {
        if (isEmpty(id)) {
            return null;
        }
        return (Resource) backend.get(IspnPK.pkResource(id));
    }

    private ResourceType getNullableResourceType(String id) {
        if (isEmpty(id)) {
            return null;
        }
        return (ResourceType) backend.get(IspnPK.pkResourceType(id));
    }

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private boolean isEmpty(Collection c) {
        return c == null || c.isEmpty();
    }

}
