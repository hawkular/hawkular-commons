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
package org.hawkular.commons.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.hawkular.commons.log.MsgLogger;
import org.hawkular.commons.log.MsgLogging;

/**
 * Read global properties from hawkular.properties file.
 *
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class HawkularProperties {
    private static final MsgLogger log = MsgLogging.getMsgLogger(HawkularProperties.class);
    private static final String CONFIG_PATH = "hawkular.configuration";
    private static final String DEFAULT_FILE = "hawkular.properties";

    private static Properties properties = null;

    private HawkularProperties() {
    }

    public static String getProperty(String key, String defaultValue) {
        return getProperty(key, null, defaultValue);
    }

    public static String getProperty(String key, String envKey, String defaultValue) {
        if (properties == null) {
            init();
        }
        String value = System.getProperty(key);
        if (value == null) {
            if (envKey != null) {
                value = System.getenv(envKey);
            }
            if (value == null) {
                value = properties.getProperty(key, defaultValue);
            }
        }
        return value;
    }

    private static void init() {
        try {
            String configPath = System.getProperty(CONFIG_PATH);
            InputStream is = null;
            if (configPath != null) {
                File configFile = new File(configPath, DEFAULT_FILE);
                if (configFile.exists() && configFile.isFile()) {
                    is = new FileInputStream(configFile);
                }
            }
            if (is == null) {
                log.debug("No properties file found. Loading from ClassLoader.");
                is = HawkularProperties.class.getClassLoader().getResourceAsStream(DEFAULT_FILE);
            }
            properties = new Properties();
            properties.load(is);
        } catch (IOException e) {
            log.error("Error loading properties.", e);
        }
    }

}
