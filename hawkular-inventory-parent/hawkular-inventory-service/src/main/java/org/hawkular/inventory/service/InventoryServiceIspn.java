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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.hawkular.commons.json.JsonUtil;
import org.hawkular.inventory.api.InventoryService;
import org.hawkular.inventory.api.ResourceFilter;
import org.hawkular.inventory.api.model.InventoryHealth;
import org.hawkular.inventory.api.model.RawResource;
import org.hawkular.inventory.api.model.Resource;
import org.hawkular.inventory.api.model.ResourceNode;
import org.hawkular.inventory.api.model.ResourceType;
import org.hawkular.inventory.api.model.ResultSet;
import org.hawkular.inventory.log.InventoryLoggers;
import org.hawkular.inventory.log.MsgLogger;
import org.hawkular.inventory.service.ispn.IspnResource;
import org.hawkular.inventory.service.ispn.IspnResourceType;
import org.infinispan.Cache;
import org.infinispan.query.Search;
import org.infinispan.query.dsl.FilterConditionContextQueryBuilder;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryBuilder;
import org.infinispan.query.dsl.QueryFactory;

import com.fasterxml.jackson.core.JsonGenerator;

/**
 * @author Joel Takvorian
 */
@Local(InventoryService.class)
@Stateless
public class InventoryServiceIspn implements InventoryService {

    private static final MsgLogger log = InventoryLoggers.getLogger(InventoryServiceIspn.class);

    // TODO [lponce] this should be configurable
    private static final int MAX_RESULTS = 100;

    @Inject
    @InventoryConfigPath
    private Path configPath;

    @Inject
    @InventoryResource
    private Cache<String, Object> resource;

    @Inject
    @InventoryResource
    private QueryFactory qResource;

    @Inject
    @InventoryResourceType
    private Cache<String, Object> resourceType;

    @Inject
    @InventoryResourceType
    private QueryFactory qResourceType;

    @EJB
    private InventoryStatsMBean inventoryStatsMBean;

    @Inject
    private ScrapeConfig scrapeConfig;

    @Inject
    @ScrapeLocation
    private File scrapeLocation;

    public InventoryServiceIspn() {
    }

    InventoryServiceIspn(Cache<String, Object> resource, Cache<String, Object> resourceType, String configPath, InventoryStatsMBean inventoryStatsMBean, ScrapeConfig scrapeConfig, File scrapeLocation) {
        this.resource = resource;
        this.resourceType = resourceType;
        qResource = Search.getQueryFactory(resource);
        qResourceType = Search.getQueryFactory(resourceType);
        this.configPath = Paths.get(configPath);
        this.inventoryStatsMBean = inventoryStatsMBean;
        this.scrapeConfig = scrapeConfig;
        this.scrapeLocation = scrapeLocation;
    }


    @Override
    public void addResource(RawResource r) {
        addResource(Collections.singletonList(r));
    }

    @Override
    public void addResource(Collection<RawResource> resources) {
        if (isEmpty(resources)) {
            return;
        }
        Map<String, IspnResource> map = resources.stream()
                .parallel()
                .peek(this::checkAgent)
                .collect(Collectors.toMap(r -> r.getId(), r -> new IspnResource(r)));
        resource.putAll(map);
    }

    @Override
    public void addResourceType(ResourceType rt) {
        addResourceType(Collections.singletonList(rt));
    }

    @Override
    public void addResourceType(Collection<ResourceType> resourceTypes) {
        if (isEmpty(resourceTypes)) {
            return;
        }
        Map<String, IspnResourceType> map = resourceTypes.stream()
                .parallel()
                .collect(Collectors.toMap(rt -> rt.getId(), rt -> new IspnResourceType(rt)));
        resourceType.putAll(map);
    }

    @Override
    public void deleteResources(Collection<String> ids) {
        if (isEmpty(ids)) {
            throw new IllegalArgumentException("Ids must be not null or empty");
        }
        ids.forEach(resource::remove);
    }

    @Override
    public void deleteAllResources() {
        resource.clear();
    }

    @Override
    public void deleteResourceTypes(Collection<String> typeIds) {
        if (isEmpty(typeIds)) {
            throw new IllegalArgumentException("Types must be not null or empty");
        }
        typeIds.forEach(resourceType::remove);
    }

    @Override
    public void deleteAllTypes() {
        resourceType.clear();
    }

    private Optional<RawResource> getRawResource(String id) {
        if (isEmpty(id)) {
            throw new IllegalArgumentException("Resource id must be not null");
        }
        return Optional.ofNullable((IspnResource) resource.get(id)).map(IspnResource::getRawResource);
    }

    @Override
    public Optional<Resource> getResourceById(String id) {
        return getRawResource(id).map(r -> Resource.fromRaw(r, this::getResourceType));
    }

    @Override
    public Optional<ResourceNode> getTree(String parentId) {
        return getRawResource(parentId)
                .map(r -> ResourceNode.fromRaw(r, this::getResourceType, this::getResourcesForParent));
    }

    @Override
    public ResultSet<Resource> getResources(ResourceFilter filter, long startOffset, int maxResults) {
        QueryBuilder qb = qResource.from(IspnResource.class);
        FilterConditionContextQueryBuilder fccqb = null;
        if (filter.isRootOnly()) {
            fccqb = qb.having("parentId").isNull();
        }
        if (filter.getTypeId() != null) {
            fccqb = (fccqb == null ? qb : fccqb.and()).having("typeId").equal(filter.getTypeId());
        }
        if (filter.getFeedId() != null) {
            fccqb = (fccqb == null ? qb : fccqb.and()).having("feedId").equal(filter.getFeedId());
        }
        Query query = (fccqb == null ? qb : fccqb)
                .maxResults(maxResults)
                .startOffset(startOffset).build();
        List<IspnResource> ispnResources = query.list();
        List<Resource> result = ispnResources
                .stream()
                .map(r -> r.toResource(this::getResourceType))
                .collect(Collectors.toList());
        return new ResultSet<>(result, (long) query.getResultSize(), startOffset);
    }

    @Override
    public ResultSet<Resource> getResources(ResourceFilter filter) {
        return getResources(filter, 0, MAX_RESULTS);
    }

    @Override
    public ResultSet<ResourceType> getResourceTypes(long startOffset, int maxResults) {
        Query query = qResourceType.from(IspnResourceType.class)
                .maxResults(maxResults)
                .startOffset(startOffset)
                .build();
        return new ResultSet<>(query.list()
                .stream()
                .map(r -> ((IspnResourceType) r).getResourceType())
                .collect(Collectors.toList()),
                (long) query.getResultSize(),
                startOffset);
    }

    @Override
    public ResultSet<ResourceType> getResourceTypes() {
        return getResourceTypes(0, MAX_RESULTS);
    }

    @Override
    public Optional<ResourceType> getResourceType(String typeId) {
        if (isEmpty(typeId)) {
            throw new IllegalArgumentException("ResourceType id must be not null");
        }
        return Optional.ofNullable((IspnResourceType) resourceType.get(typeId)).map(IspnResourceType::getResourceType);
    }

    @Override
    public Optional<String> getAgentConfig(String templateName) {
        return getConfig(templateName + "-inventory.yml");
    }

    @Override
    public Optional<String> getJMXExporterConfig(String templateName) {
        return getConfig(templateName + "-jmx-exporter.yml");
    }

    @Override
    public boolean isRunning() {
        if (resource != null
                && resourceType != null
                && resource.getStatus() != null
                && resourceType.getStatus() != null
                && resource.getStatus().allowInvocations()
                && resourceType.getStatus().allowInvocations()) {
            return true;
        }
        return false;
    }

    private Optional<String> getConfig(String fileName) {
        if (fileName.contains("..")) {
            throw new IllegalArgumentException("Cannot get file with '..' in path: " + fileName);
        }

        try {
            byte[] encoded = Files.readAllBytes(configPath.resolve(fileName));
            return Optional.of(new String(encoded, StandardCharsets.UTF_8.name()));
        } catch (IOException ioe) {
            try {
                InputStream is = this.getClass().getClassLoader().getResourceAsStream(fileName);
                String text = null;
                try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
                    text = scanner.useDelimiter("\\A").next();
                    if (scanner.ioException() != null) {
                        throw scanner.ioException();
                    }
                    return Optional.of(text);
                }
            } catch (Exception e) {
                return Optional.empty();
            }
        }
    }

    @Override
    public ResultSet<Resource> getChildren(String parentId) {
        return getChildren(parentId, 0, MAX_RESULTS);
    }

    @Override
    public ResultSet<Resource> getChildren(String parentId, long startOffset, int maxResults) {
        Query query = qResource.from(IspnResource.class)
                .having("parentId").equal(parentId)
                .maxResults(maxResults)
                .startOffset(startOffset).build();
        List<IspnResource> ispnResources = query.list();
        List<Resource> result = ispnResources
                .stream()
                .map(r -> r.toResource(this::getResourceType))
                .collect(Collectors.toList());
        return new ResultSet<>(result, (long) query.getResultSize(), startOffset);
    }

    private List<RawResource> getResourcesForParent(String parentId) {
        if (isEmpty(parentId)) {
            return Collections.emptyList();
        }
        return qResource.from(IspnResource.class)
                .having("parentId").equal(parentId)
                .build()
                .list()
                .stream()
                .map(r -> ((IspnResource) r).getRawResource())
                .collect(Collectors.toList());
    }

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

    private boolean isEmpty(Collection c) {
        return c == null || c.isEmpty();
    }

    @Override
    public void buildExport(OutputStream os) throws IOException {
        JsonGenerator jsonGen = JsonUtil.createJsonGenerator(os);
        jsonGen.writeStartObject(); // Inventory object
        jsonGen.writeFieldName("types");
        jsonGen.writeStartArray();

        int offset = 0;
        boolean hasMore = true;
        while (hasMore) {
            List<IspnResourceType> batch = qResourceType.from(IspnResourceType.class)
                    .maxResults(MAX_RESULTS)
                    .startOffset(offset)
                    .build()
                    .list();
            for (IspnResourceType rt : batch) {
                jsonGen.writeObject(rt.getResourceType());
            }
            jsonGen.flush();
            hasMore = batch.size() == MAX_RESULTS;
            offset += MAX_RESULTS;
        }
        jsonGen.writeEndArray();
        jsonGen.writeFieldName("resources");
        jsonGen.writeStartArray();
        hasMore = true;
        offset = 0;
        while (hasMore) {
            List<IspnResource> batch = qResource.from(IspnResource.class)
                    .maxResults(MAX_RESULTS)
                    .startOffset(offset)
                    .build()
                    .list();
            for (IspnResource r : batch) {
                jsonGen.writeObject(r.getRawResource());
            }
            jsonGen.flush();
            hasMore = batch.size() == MAX_RESULTS;
            offset += MAX_RESULTS;
        }
        jsonGen.writeEndArray();
        jsonGen.writeEndObject();
        jsonGen.flush();
    }

    @Override
    public InventoryHealth getHealthStatus() {
        return inventoryStatsMBean.lastHealth();
    }

    @Override
    public void buildMetricsEndpoints() {
        for (Map.Entry<String, String> filter : scrapeConfig.getFilter().entrySet()) {
            int nResults, offSet = 0;
            do {
                Query qb = qResource.from(IspnResource.class)
                        .having("typeId")
                        .equal(filter.getKey())
                        .startOffset(offSet)
                        .maxResults(MAX_RESULTS)
                        .build();
                nResults = qb.getResultSize();
                List<IspnResource> results = qb.list();
                offSet = results.size();
                results.forEach(r -> writeMetricsEndpoint(r.getRawResource()));
            } while (offSet < nResults);
        }
    }

    private void checkAgent(RawResource rawResource) {
        if (scrapeConfig.filter(rawResource)) {
            writeMetricsEndpoint(rawResource);
        }
    }

    private void writeMetricsEndpoint(RawResource rawResource) {
        String feedId = rawResource.getFeedId();
        String metricsEndpoint = rawResource.getConfig().get(scrapeConfig.getFilter().get(rawResource.getTypeId()));

        if (isEmpty(feedId) || isEmpty(metricsEndpoint)) {
            log.errorMissingInfoInAgentRegistration(rawResource.getId());
            return;
        }

        if (feedId.contains("..")) {
            throw new IllegalArgumentException("Cannot write metrics endpoint file with '..' in path: " + feedId);
        }

        // Prometheus file format. See: https://prometheus.io/docs/operating/configuration/#<file_sd_config>
        String content = String.format("[ { \"targets\": [ \"%s\" ], \"labels\": { \"feed-id\": \"%s\" } } ]",
                metricsEndpoint,
                feedId);
        try {
            File newScrapeConfig = new File(scrapeLocation, feedId + ".json");
            Files.write(newScrapeConfig.toPath(), content.getBytes(StandardCharsets.UTF_8));
            log.infoRegisteredMetricsEndpoint(feedId, metricsEndpoint, newScrapeConfig.toString());
        } catch (Exception e) {
            log.errorCannotRegisterMetricsEndpoint(feedId, metricsEndpoint, e);
        }
    }



}
