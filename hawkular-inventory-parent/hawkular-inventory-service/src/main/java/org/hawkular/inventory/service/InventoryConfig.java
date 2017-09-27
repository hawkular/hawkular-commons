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
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.inject.Produces;

import org.hawkular.inventory.log.InventoryLoggers;
import org.hawkular.inventory.log.MsgLogger;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.query.Search;
import org.infinispan.query.dsl.QueryFactory;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@Startup
@Singleton
public class InventoryConfig {

    public static final String CACHE_CONFIGURATION = "hawkular-inventory-ispn.xml";
    public static final String RESOURCE_CACHE_NAME = "resource";
    public static final String RESOURCE_TYPE_CACHE_NAME = "resource_type";

    private static final MsgLogger log = InventoryLoggers.getLogger(InventoryConfig.class);

    private Cache<String, Object> resource;
    private QueryFactory queryResource;

    private Cache<String, Object> resourceType;
    private QueryFactory queryResourceType;

    private final Path configPath;

    public InventoryConfig() {
        configPath = Paths.get(System.getProperty("jboss.server.config.dir"), "hawkular");
    }

    @PostConstruct
    public void init() {
        EmbeddedCacheManager cacheManager;
        try {
            File cacheConfigFile = new File(configPath.toFile(), CACHE_CONFIGURATION);
            if (cacheConfigFile.exists()) {
                cacheManager = new DefaultCacheManager(cacheConfigFile.getPath());
            } else {
                cacheManager =
                        new DefaultCacheManager(InventoryConfig.class.getResourceAsStream("/" + CACHE_CONFIGURATION));
            }
            resource = cacheManager.getCache(RESOURCE_CACHE_NAME);
            if (resource == null) {
                log.errorInventoryCacheNotFound();
                throw new IllegalStateException("Inventory backend resource cache is not found");
            }
            queryResource = Search.getQueryFactory(resource);
            if (queryResource == null) {
                log.errorInventoryCacheNotFound();
                throw new IllegalStateException("Inventory query factory for resource cache is not found");
            }
            resourceType = cacheManager.getCache(RESOURCE_TYPE_CACHE_NAME);
            if (resourceType == null) {
                log.errorInventoryCacheNotFound();
                throw new IllegalStateException("Inventory backend resource_type cache is not found");
            }
            queryResourceType = Search.getQueryFactory(resourceType);
            if (queryResourceType == null) {
                log.errorInventoryCacheNotFound();
                throw new IllegalStateException("Inventory query factory for resource_type cache is not found");
            }
        } catch (IOException e) {
            log.errorInventoryCacheConfigurationNotFound(e);
        }
    }

    @Produces
    @InventoryResource
    public Cache<String, Object> getResourceCache() {
        return resource;
    }

    @Produces
    @InventoryResource
    public QueryFactory getResourceQueryFactory() {
        return queryResource;
    }

    @Produces
    @InventoryResourceType
    public Cache<String, Object> getResourceTypeCache() {
        return resourceType;
    }

    @Produces
    @InventoryResourceType
    public QueryFactory getResourceTypeQueryFactory() {
        return queryResourceType;
    }

}
