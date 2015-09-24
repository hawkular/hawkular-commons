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

public class EmbeddedConstants {
    public static final String CASSANDRA_CONFIG = "cassandra.config";
    public static final String CASSANDRA_LISTEN_ADDRESS_DEFAULT = "127.0.0.1";
    public static final Integer CASSANDRA_NATIVE_PORT_DEFAULT = 9042;
    public static final String CASSANDRA_YAML = "cassandra.yaml";
    public static final String JBOSS_DATA_DIR = "jboss.server.data.dir";
    public static final String HAWKULAR_DATA = "hawkular-data";
    public static final String EMBEDDED_CASSANDRA_OPTION = "embedded_cassandra";
    public static final String HAWKULAR_BACKEND_PROPERTY = "hawkular.backend";
    public static final String HAWKULAR_BACKEND_ENV_NAME = "HAWKULAR_BACKEND";

    private EmbeddedConstants() {
        // Constants class
    }
}
