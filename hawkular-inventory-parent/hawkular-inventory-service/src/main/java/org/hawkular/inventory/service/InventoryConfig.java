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

    private static final MsgLogger log = InventoryLoggers.getLogger(InventoryConfig.class);
    public static final String CACHE_CONFIGURATION = "/hawkular-inventory-ispn.xml";
    public static final String CACHE_NAME = "backend";

    private Cache<String, Object> backend;
    private QueryFactory queryFactory;

    @PostConstruct
    public void init() {
        try {
            EmbeddedCacheManager cacheManager =
                    new DefaultCacheManager(InventoryConfig.class.getResourceAsStream(CACHE_CONFIGURATION));
            backend = cacheManager.getCache(CACHE_NAME);
            if (backend == null) {
                log.errorInventoryCacheNotFound();
                throw new IllegalStateException("Inventory backend cache is not found");
            }
            queryFactory = Search.getQueryFactory(backend);
            if (queryFactory == null) {
                log.errorInventoryCacheNotFound();
                throw new IllegalStateException("Inventory query factory cache is not found");
            }
        } catch (IOException e) {
            log.errorInventoryCacheConfigurationNotFound(e);
        }
    }

    @Produces
    @InventoryCache
    public Cache<String, Object> getInventoryCache() {
        return backend;
    }

    @Produces
    @InventoryCache
    public QueryFactory getQueryFactory() {
        return queryFactory;
    }
}
