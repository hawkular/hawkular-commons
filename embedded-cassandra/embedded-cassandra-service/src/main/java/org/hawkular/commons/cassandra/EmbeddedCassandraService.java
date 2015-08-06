/*
 * Copyright 2014-2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.commons.cassandra;

import static org.hawkular.commons.cassandra.EmbeddedConstants.EMBEDDED_CASSANDRA_OPTION;
import static org.hawkular.commons.cassandra.EmbeddedConstants.HAWKULAR_BACKEND_ENV_NAME;
import static org.hawkular.commons.cassandra.EmbeddedConstants.HAWKULAR_BACKEND_PROPERTY;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.cassandra.service.CassandraDaemon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * App initialization
 *
 * @author Stefan Negrea
 */
@Startup
@Singleton
public class EmbeddedCassandraService {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedCassandraService.class);


    private CassandraDaemon cassandraDaemon;

    public EmbeddedCassandraService() {
        logger.info("======== Hawkular - Embedded Cassandra ========");
    }

    @PostConstruct
    public void start() {
        synchronized (this) {
            String backend = System.getProperty(HAWKULAR_BACKEND_PROPERTY);

            String tmp = System.getenv(HAWKULAR_BACKEND_ENV_NAME);
            if (tmp!=null) {
                backend = tmp;
                logger.debug("== Using backend setting from environment: " + tmp);
            }
            if (cassandraDaemon == null && EMBEDDED_CASSANDRA_OPTION.equals(backend)) {
                try {
                    ConfigEditor editor = new ConfigEditor();
                    editor.initEmbeddedConfiguration();

                    cassandraDaemon = new CassandraDaemon();
                    cassandraDaemon.activate();
                } catch (Exception e) {
                    logger.error("Error initializing embedded Cassandra server", e);
                }
            }
            else {
                logger.info("== Embedded Cassandra not started as selected backend was " + backend + " ==");
            }
        }
    }

    @PreDestroy
    void stop() {
        synchronized (this) {
            if (cassandraDaemon != null) {
                cassandraDaemon.deactivate();
            }
        }
    }
}
