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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.hawkular.inventory.api.InventoryService;
import org.hawkular.inventory.api.ResourceNode;
import org.hawkular.inventory.log.InventoryLoggers;
import org.hawkular.inventory.log.MsgLogger;
import org.hawkular.inventory.model.Metric;
import org.hawkular.inventory.model.Resource;
import org.hawkular.inventory.model.ResourceType;
import org.infinispan.Cache;
import org.infinispan.query.dsl.QueryFactory;

/**
 * @author Joel Takvorian
 */
@Local(InventoryService.class)
@Stateless
public class InventoryServiceIspn implements InventoryService {
    private static final MsgLogger log = InventoryLoggers.getLogger(InventoryServiceIspn.class);

    @Inject
    @InventoryCache
    private Cache backend;

    @Inject
    @InventoryCache
    private QueryFactory queryFactory;

    // Temp (ispn mock: stores)
    private Collection<ResourceType> allResourceTypes = new ArrayList<>();

    // Temp (ispn mock: indexes)
    private Map<String, List<Resource>> resourcesByType = new HashMap<>();
    private Map<String, Metric> metricsById = new HashMap<>();

    @Override
    public void addResource(Resource r) {
        if (isEmpty(r)) {
            throw new IllegalArgumentException("Resource must be not null");
        }
        backend.put(IspnPK.pk(r), r);
    }

    @Override
    public void addMetric(Metric m) {
        if (isEmpty(m)) {
            throw new IllegalArgumentException("Metric must be not null");
        }
        backend.put(IspnPK.pk(m), m);
    }

    @Override
    public void addResourceType(ResourceType rt) {
        if (isEmpty(rt)) {
            throw new IllegalArgumentException("ResourceType must be not null");
        }
        backend.put(IspnPK.pk(rt), rt);
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
                        this::getNullableResource,
                        this::getNullableMetric));
    }

    @Override
    public Collection<Resource> getAllTopResources() {
        return queryFactory.from(Resource.class)
                .having("rootId").isNull()
                .toBuilder().build().list();
    }

    @Override
    public Collection<ResourceType> getAllResourceTypes() {
        return queryFactory.from(ResourceType.class)
                .build().list();
    }

    @Override
    public Collection<Resource> getResourcesByType(String typeId) {
        return queryFactory.from(Resource.class)
                .having("typeId").equal(typeId)
                .toBuilder().build().list();
    }

    @Override
    public Optional<Collection<Metric>> getResourceMetrics(String id) {
        return getResourceById(id)
                .map(r -> r.getMetrics(metricId -> (Metric) backend.get(IspnPK.pkMetric(metricId))));
    }

    @Override
    public Optional<ResourceType> getResourceType(String typeId) {
        if (isEmpty(typeId)) {
            throw new IllegalArgumentException("ResourceType id must be not null");
        }
        return Optional.ofNullable((ResourceType) backend.get(IspnPK.pkResourceType(typeId)));
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

    private Metric getNullableMetric(String id) {
        if (isEmpty(id)) {
            return null;
        }
        return (Metric) backend.get(IspnPK.pkMetric(id));
    }

    private void checkBackend() {

    }

    private boolean isEmpty(Resource r) {
        return r == null || r.getId() == null || r.getId().isEmpty();
    }

    private boolean isEmpty(Metric m) {
        return m == null || m.getId() == null || m.getId().isEmpty();
    }

    private boolean isEmpty(ResourceType rt) {
        return rt == null || rt.getId() == null || rt.getId().isEmpty();
    }

    private boolean isEmpty(String s) {
        return s == null || s.isEmpty();
    }

}
