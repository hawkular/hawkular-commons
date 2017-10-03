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
package org.hawkular.commons.cassandra;

import static org.hawkular.commons.cassandra.EmbeddedConstants.CASSANDRA_CONFIG;
import static org.hawkular.commons.cassandra.EmbeddedConstants.CASSANDRA_TRIGGERS_DIR;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * A set of fluent builders to prepare a {@code cassandra.yaml} file to be used by embedded Cassandra. Typical usage:
 *
 * <pre>
 * CassandraYaml.builder()
 *         .load("/path/to/default/cassandra.yaml")//
 *         .baseDir("/set/all/datadirs/releative/to/this/dir")//
 *         .clusterName("myCluster")//
 *         .defaultKeyCacheSize()//
 *         .defaultNativeTransportMaxThreads()//
 *         .store("/path/to/cassandra.yaml")//
 *         .mkdirs()//
 *         .setCassandraConfigProp()
 *         .setTriggersDirProp();
 * </pre>
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 *
 */
public class CassandraYaml {
    /**
     * A fluent builder to prepare a {@code cassandra.yaml} file.
     */
    public static class CassandraYamlBuilder {
        /** A pattern to strip {@code _directory} suffix from {@link CassandraYaml} keys */
        private static final Pattern DIRECTORY_SUFFIX_PATTERN = Pattern.compile("_directory$");

        public static DumperOptions createDefaultDumperOptions() {
            DumperOptions result = new DumperOptions();
            result.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            return result;
        }

        private final Map<String, Object> config = new TreeMap<>();

        private File triggersDir;

        private final Yaml yaml = new Yaml(createDefaultDumperOptions());

        /**
         * You are probably looking for {@link CassandraYaml#builder()}.
         */
        private CassandraYamlBuilder() {
            super();
        }

        /**
         * Set various directories relative to the given {@code baseDir}.
         *
         * @param baseDir the directory to use a root for the Cassandra cluster data
         * @return this {@link CassandraYamlBuilder}
         */
        public CassandraYamlBuilder baseDir(final File baseDir) {

            config.put(CassandraYamlKey.data_file_directories.name(),
                    Collections.singletonList(new File(baseDir, "data").getAbsolutePath()));

            Stream.of(CassandraYamlKey.DIRECTORY_KEYS).forEach(key -> {
                final String keyName = key.name();
                File dir = new File(baseDir, DIRECTORY_SUFFIX_PATTERN.matcher(keyName).replaceAll(""));
                config.put(keyName, dir.getAbsolutePath());
            });

            triggersDir(new File(baseDir, "triggers"));

            return this;
        }

        /**
         * Sets {@link #triggersDir} to value passed through the parameter {@code triggersDir}
         *
         * @param triggersDir the path to triggers directory
         * @return this {@link CassandraYamlBuilder}
         */
        public CassandraYamlBuilder triggersDir(File triggersDir) {
            this.triggersDir = triggersDir;
            return this;
        }

        /**
         * @param clusterName the cluster name to set
         * @return this {@link CassandraYamlBuilder}
         */
        public CassandraYamlBuilder clusterName(final String clusterName) {
            return opt(CassandraYamlKey.cluster_name, clusterName);
        }

        /**
         * Sets {@code key_cache_size_in_mb} to {@code min(1% of Heap (in MB), 10 MB)}.
         *
         * @return this {@link CassandraYamlBuilder}
         */
        public CassandraYamlBuilder defaultKeyCacheSize() {
            int defaultKeyCacheSize =
                    Math.min(Math.max(1, (int) (Runtime.getRuntime().totalMemory() * 0.01 / 1024 / 1024)), 10);
            return opt(CassandraYamlKey.key_cache_size_in_mb, defaultKeyCacheSize);
        }

        /**
         * Sets {@code native_transport_max_threads} to
         * {@code Math.max(1, Runtime.getRuntime().availableProcessors() / 2)}
         *
         * @return this {@link CassandraYamlBuilder}
         */
        public CassandraYamlBuilder defaultNativeTransportMaxThreads() {
            int defaultNativeTransportMaxThreads =
                    Math.max(1, Runtime.getRuntime().availableProcessors() / 2);
            return opt(CassandraYamlKey.native_transport_max_threads, defaultNativeTransportMaxThreads);
        }

        private List<File> getDirectoriesToCreate() {
            List<File> result = new ArrayList<>(2 + CassandraYamlKey.DIRECTORY_KEYS.length);
            Stream.of(CassandraYamlKey.DIRECTORY_KEYS).forEach(key -> {
                final String keyName = key.name();
                Object path = config.get(keyName);
                if (path instanceof String) {
                    File dir = new File((String) path);
                    result.add(dir);
                }
            });

            Object dataDirs = config.get(CassandraYamlKey.data_file_directories.name());
            if (dataDirs instanceof List<?>) {
                for (Object path : (List<?>) dataDirs) {
                    if (path instanceof String) {
                        File dir = new File((String) path);
                        result.add(dir);
                    }
                }
            }

            if (triggersDir != null) {
                result.add(triggersDir);
            }

            return Collections.unmodifiableList(result);
        }

        /**
         * Load the config from an existing {@code cassandra.yaml} file overriding the pre-existing values in this
         * {@link CassandraYamlBuilder}.
         *
         * @param file the {@code cassandra.yaml} file to load
         * @return this {@link CassandraYamlBuilder}
         * @throws IOException
         */
        public CassandraYamlBuilder load(File file) throws IOException {
            try (InputStream in = new FileInputStream(file)) {
                return load(in);
            }
        }

        /**
         * Load the config from an existing {@code cassandra.yaml} stream overriding the pre-existing values in this
         * {@link CassandraYamlBuilder}.
         *
         * @param in the stream to load
         * @return this {@link CassandraYamlBuilder}
         * @throws IOException
         */
        public CassandraYamlBuilder load(InputStream in) throws IOException {
            try (Reader r = new InputStreamReader(in, "utf-8")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fileValues = (Map<String, Object>) yaml.load(r);
                config.putAll(fileValues);
            }
            return this;
        }

        /**
         * Load the config from an existing {@code cassandra.yaml} URL overriding the pre-existing values in this
         * {@link CassandraYamlBuilder}.
         *
         * @param url the stream to load
         * @return this {@link CassandraYamlBuilder}
         * @throws IOException
         */
        public CassandraYamlBuilder load(URL url) throws IOException {
            return load(url.openStream());
        }

        /**
         * Set the given {@code} to the given {@code value}
         *
         * @param key
         * @param value
         * @return this {@link CassandraYamlBuilder}
         */
        public CassandraYamlBuilder opt(CassandraYamlKey key, Object value) {
            config.put(key.name(), value);
            return this;
        }

        /**
         * Shift all ports by {@code portOffset}
         *
         * @param portOffset
         * @return this {@link CassandraYamlBuilder}
         */
        public CassandraYamlBuilder portOffset(final int portOffset) {

            Stream.of(CassandraYamlKey.PORT_KEYS).forEach(key -> {
                final String keyName = key.name();
                int defaultValue = ((Integer) key.defaultValue).intValue();
                config.put(keyName, Integer.valueOf(defaultValue + portOffset));
            });

            return this;
        }

        /**
         * Stores the prepared {@code cassandra.yaml} file under the given path.
         *
         * @param path the path where to store the {@code cassandra.yaml} file
         * @return this {@link CassandraYamlBuilder}
         * @throws IOException
         */
        public StoredCassandraYamlStage store(File path) throws IOException {
            final File parentDir = path.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }
            String cassandraConfigUrl = path.toURI().toURL().toString();
            try (Writer w = new OutputStreamWriter(new FileOutputStream(path), "utf-8")) {
                yaml.dump(config, w);
            }
            return new StoredCassandraYamlStage(getDirectoriesToCreate(), cassandraConfigUrl,
                    triggersDir.getAbsolutePath());
        }

    }

    /**
     * Some keys from {@code cassandra.yaml} configuration file.
     */
    public enum CassandraYamlKey {
        authenticator,
        cluster_name,
        commitlog_directory,
        commitlog_sync,
        commitlog_sync_period_in_ms,
        compaction_throughput_mb_per_sec,
        data_file_directories,
        endpoint_snitch,
        hinted_handoff_enabled,
        hints_directory,
        key_cache_size_in_mb,
        listen_address("127.0.0.1"),
        native_transport_max_threads,
        native_transport_port(9042),
        num_tokens,
        partitioner,
        rpc_address,
        rpc_port(9160),
        rpc_server_type,
        saved_caches_directory,
        ssl_storage_port(7000),
        start_rpc,
        storage_port(7001);

        /** Directories that we set relative to {@link CassandraYamlBuilder#baseDir(File)} */
        private static final CassandraYamlKey[] DIRECTORY_KEYS =
                new CassandraYamlKey[] { commitlog_directory, saved_caches_directory, hints_directory };

        /**
         * Keys that store various port numbers that we shift by {@code portOffset} in
         * {@link CassandraYamlBuilder#portOffset(int)}
         */
        private static final CassandraYamlKey[] PORT_KEYS =
                new CassandraYamlKey[] { native_transport_port, rpc_port, ssl_storage_port, storage_port };

        private final Object defaultValue;

        private CassandraYamlKey() {
            this.defaultValue = null;
        }

        private CassandraYamlKey(Object defaultValue) {
            this.defaultValue = defaultValue;
        }

        /**
         * @return the default value of this key, possibly {@code null}
         */
        @SuppressWarnings("unchecked")
        public <T> T getDefaultValue() {
            return (T) defaultValue;
        }
    }

    /**
     * The type returned by {@link CassandraYamlBuilder#store(File)}, that allows for performing further operations
     * related to the stored {@code cassandra.yaml} file.
     */
    public static class StoredCassandraYamlStage {
        private final String cassandraConfigUrl;
        private final List<File> directoriesToCreate;
        private final String triggersDir;

        private StoredCassandraYamlStage(List<File> directoriesToCreate, String cassandraConfigUrl,
                String triggersDir) {
            super();
            this.directoriesToCreate = directoriesToCreate;
            this.cassandraConfigUrl = cassandraConfigUrl;
            this.triggersDir = triggersDir;
        }

        /**
         * Creates all data directories (see {@link CassandraYamlKey#DIRECTORY_KEYS}) if necessary.
         *
         * @return this StoredCassandraYamlStage
         */
        public StoredCassandraYamlStage mkdirs() {
            directoriesToCreate.stream()//
                    .filter(dir -> !dir.exists())//
                    .forEach(File::mkdirs);

            return this;
        }

        /**
         * Sets "{@value CASSANDRA_CONFIG}" to file URL used in {@link CassandraYamlBuilder#store(File)}
         *
         * @return this StoredCassandraYamlStage
         */
        public StoredCassandraYamlStage setCassandraConfigProp() {
            System.setProperty(CASSANDRA_CONFIG, cassandraConfigUrl);
            return this;
        }

        /**
         * Sets "{@value CASSANDRA_TRIGGERS_DIR}" to path set through {@link CassandraYamlBuilder#baseDir(File)} or
         * {@link CassandraYamlBuilder#triggersDir(File)}.
         *
         * @return this StoredCassandraYamlStage
         */
        public StoredCassandraYamlStage setTriggersDirProp() {
            System.setProperty(CASSANDRA_TRIGGERS_DIR, triggersDir);
            return this;
        }
    }

    /**
     * @return a new {@link CassandraYamlBuilder}
     */
    public static CassandraYamlBuilder builder() {
        return new CassandraYamlBuilder();
    }

}
