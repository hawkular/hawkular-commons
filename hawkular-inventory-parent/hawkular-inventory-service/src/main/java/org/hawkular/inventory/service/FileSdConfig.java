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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hawkular.commons.json.JsonUtil;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A class that creates strings appropriate for Prometheus file_sd_config files.
 * See: https://prometheus.io/docs/operating/configuration/#<file_sd_config>
 *
 * The JSON looks like:
 *
 * [
 *   {
 *     "targets": [ "<host>", ... ],
 *     "labels": {
 *       "<labelname>": "<labelvalue>", ...
 *     }
 *   },
 *   ...
 * ]
 *
 */
public class FileSdConfig {

    public static class Entry {
        /**
         * Builds an entry from a string that has the format "host:port{label1=label2,...}".
         * @param entryString entry in the form of a string
         * @return the entry
         */
        public static Entry buildFromString(String entryString) {
            Pattern main = Pattern.compile("(.+:\\d+)(?:\\{(.+=.+(?:,\\s*.+=.+)*)\\})?");
            Matcher mainMatcher = main.matcher(entryString);
            if (!mainMatcher.matches()) {
                throw new IllegalArgumentException("Invalid entry string: " + entryString);
            }

            Entry entry = new Entry();
            entry.addTarget(mainMatcher.group(1));

            String labelString = mainMatcher.group(2);
            if (labelString != null && !labelString.isEmpty()) {
                String[] nameValuePairs = labelString.split(",\\s*");
                for (String nameValuePair : nameValuePairs) {
                    String[] nameValue = nameValuePair.split("=");
                    entry.addLabel(nameValue[0], nameValue[1]);
                }
            }

            return entry;
        }

        @JsonInclude
        private final List<String> targets = new ArrayList<>();

        @JsonInclude
        private final Map<String, String> labels = new HashMap<>();

        public Entry() {
        }

        public void addTarget(String target) {
            targets.add(target);
        }

        public void addLabel(String name, String value) {
            labels.put(name, value);
        }

        public List<String> getTargets() {
            return targets;
        }

        public Map<String, String> getLabels() {
            return labels;
        }
    }

    private final List<Entry> entries = new ArrayList<>();

    public FileSdConfig() {
    }

    public void addEntry(Entry entry) {
        this.entries.add(entry);
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public String toJson() {
        ObjectMapper mapper = JsonUtil.getMapper();
        try {
            return mapper.writeValueAsString(this.entries);
        } catch (Exception e) {
            throw new IllegalStateException("The entries are invalid: " + this.entries);
        }
    }

    public static FileSdConfig fromJson(String json) {
        ObjectMapper mapper = JsonUtil.getMapper();
        try {
            Entry[] entries = mapper.readValue(json, Entry[].class);
            FileSdConfig config = new FileSdConfig();
            for (Entry e : entries) {
                config.addEntry(e);
            }
            return config;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot deserialize json for FileSdConfig: " + json);
        }
    }
}
