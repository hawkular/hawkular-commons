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
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Files;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.management.ObjectName;

import org.hawkular.inventory.api.model.InventoryHealth;
import org.hawkular.inventory.log.InventoryLoggers;
import org.hawkular.inventory.log.MsgLogger;
import org.infinispan.Cache;
import org.infinispan.stats.Stats;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@Singleton
@Startup
public class InventoryStats implements InventoryStatsMBean {
    private static final MsgLogger log = InventoryLoggers.getLogger(InventoryStats.class);

    private static final String MBEAN_NAME = "org.hawkular:name=InventoryStats";
    private ObjectName objectName;

    private InventoryHealth health;
    private long lastUpdate;

    @Inject
    @InventoryResource
    private Cache<String, Object> resource;

    @Inject
    @InventoryResourceType
    private Cache<String, Object> resourceType;

    @Inject
    @InventoryLocation
    private File inventoryLocation;

    public InventoryStats() {
        health = null;
        lastUpdate = System.currentTimeMillis();
    }

    public InventoryStats(Cache<String, Object> resource, Cache<String, Object> resourceType, File inventoryLocation) {
        this();
        this.resource = resource;
        this.resourceType = resourceType;
        this.inventoryLocation = inventoryLocation;
    }

    @PostConstruct
    public void init() {
        try {
            objectName = new ObjectName(MBEAN_NAME);
            ManagementFactory.getPlatformMBeanServer().registerMBean(this, objectName);
        } catch (Exception e) {
            log.errorRegisteringMBean(e);
        }
    }

    @PreDestroy
    public void stop() {
        try {
            ManagementFactory.getPlatformMBeanServer().unregisterMBean(objectName);
        } catch (Exception e) {
            log.errorUnregisteringMBean(e);
        }
    }

    @Override
    public long getTimestamp() {
        refresh();
        return health.getTimestamp();
    }

    @Override
    public long getInventoryTotalDiskSpace() {
        refresh();
        return health.getDiskStats().getInventoryTotalSpace();
    }

    @Override
    public long getInventoryFreeDiskSpace() {
        refresh();
        return health.getDiskStats().getInventoryFreeSpace();
    }

    @Override
    public long getNumberOfResources() {
        refresh();
        return health.getInventoryStats().getNumberOfResources();
    }

    @Override
    public long getNumberOfResourcesInMemory() {
        refresh();
        return health.getInventoryStats().getNumberOfResourcesInMemory();
    }

    @Override
    public long getAverageReadTimeForResources() {
        refresh();
        return health.getInventoryStats().getAverageReadTimeForResources();
    }

    @Override
    public long getAverageWriteTimeForResources() {
        refresh();
        return health.getInventoryStats().getAverageWriteTimeForResources();
    }

    @Override
    public long getNumberOfResourceTypes() {
        refresh();
        return health.getInventoryStats().getNumberOfResourceTypes();
    }

    @Override
    public long getNumberOfResourceTypesInMemory() {
        refresh();
        return health.getInventoryStats().getNumberOfResourceTypeInMemory();
    }

    @Override
    public long getAverageReadTimeForResourceTypes() {
        refresh();
        return health.getInventoryStats().getAverageReadTimeForResourceTypes();
    }

    @Override
    public long getAverageWriteTimeForResourceTypes() {
        refresh();
        return health.getInventoryStats().getAverageWriteTimeForResourceTypes();
    }

    @Override
    public InventoryHealth lastHealth() {
        refresh();
        return health;
    }

    private void refresh() {
        long now = System.currentTimeMillis();
        if (health == null || ((now - lastUpdate) > 1000)) {
            InventoryHealth.Builder builder = InventoryHealth.builder();
            MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
            builder.timestamp(System.currentTimeMillis());
            builder.heapMemoryUsage(memoryMXBean.getHeapMemoryUsage());
            builder.nonHeapMemoryUsage(memoryMXBean.getNonHeapMemoryUsage());
            builder.systemLoadAverage(ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage());
            List<GarbageCollectorMXBean> gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
            builder.gcCollectionCount(gcMXBeans.stream()
                    .mapToLong(gc -> gc.getCollectionCount())
                    .sum());
            builder.gcCollectionTime(gcMXBeans.stream()
                    .mapToLong(gc -> gc.getCollectionTime())
                    .sum());
            try {
                builder.inventoryTotalSpace(Files.walk(inventoryLocation.toPath())
                        .map(f -> f.toFile())
                        .filter(f -> f.isFile())
                        .mapToLong( f -> f.length())
                        .sum());
                builder.inventoryFreeSpace(inventoryLocation.getUsableSpace());
            } catch (Exception e) {
                log.errorReadingInventoryDisk(e);
            }
            Stats resourceStats = resource.getAdvancedCache().getStats();
            builder.numberOfResources(resourceStats.getCurrentNumberOfEntries());
            builder.numberOfResourcesInMemory(resourceStats.getCurrentNumberOfEntriesInMemory());
            builder.averageReadTimeForResources(resourceStats.getAverageReadTime());
            builder.averageWriteTimeForResources(resourceStats.getAverageWriteTime());
            Stats resourceTypeStats = resourceType.getAdvancedCache().getStats();
            builder.numberOfResourceTypes(resourceTypeStats.getCurrentNumberOfEntries());
            builder.numberOfResourceTypesInMemory(resourceTypeStats.getCurrentNumberOfEntriesInMemory());
            builder.averageReadTimeForResourceTypes(resourceTypeStats.getAverageReadTime());
            builder.averageWriteTimeForResourceTypes(resourceTypeStats.getAverageWriteTime());
            health = builder.build();
        }
    }
}
