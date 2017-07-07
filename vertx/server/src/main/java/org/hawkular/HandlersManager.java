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
package org.hawkular;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.hawkular.commons.log.MsgLogger;
import org.hawkular.commons.log.MsgLogging;
import org.hawkular.commons.properties.HawkularProperties;
import org.hawkular.handlers.BaseApplication;
import org.hawkular.handlers.RestEndpoint;
import org.hawkular.handlers.RestHandler;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class HandlersManager {
    private static final MsgLogger log = MsgLogging.getMsgLogger(HandlersManager.class);

    // For pattern examples see:
    // github.com/vert-x3/vertx-web/blob/master/vertx-web/src/test/java/io/vertx/ext/web/handler/CORSHandlerTest.java
    private static final String CORS_ORIGIN_PATTERN = "hawkular.cors-origin-pattern";
    private static final String CORS_ORIGIN_PATTERN_DEFAULT = "*";
    private static final String CORS_HEADERS = "hawkular.cors-headers";
    private static final String CORS_HEADERS_DEFAULT = "origin,accept,content-type,hawkular-tenant";
    private static final String CORS_METHODS = "hawkular.cors-methods";
    private static final String CORS_METHODS_DEFAULT = "GET,POST,PUT,PATCH,DELETE,OPTIONS,HEAD";

    private Router router;
    private Map<String, BaseApplication> applications = new HashMap<>();
    private Map<String, Class<BaseApplication>> applicationsClasses = new HashMap<>();
    private Map<EndpointKey, RestHandler> endpoints = new HashMap<>();
    private Map<EndpointKey, Class<RestHandler>> endpointsClasses = new HashMap<>();
    private ClassLoader cl = Thread.currentThread().getContextClassLoader();
    private String corsAllowedOriginPattern;
    private String corsHeaders;
    private String corsMethods;

    public HandlersManager(Vertx vertx) {
        this.router = Router.router(vertx);
        corsAllowedOriginPattern = HawkularProperties.getProperty(CORS_ORIGIN_PATTERN, CORS_ORIGIN_PATTERN_DEFAULT);
        corsHeaders = HawkularProperties.getProperty(CORS_HEADERS, CORS_HEADERS_DEFAULT);
        corsMethods = HawkularProperties.getProperty(CORS_METHODS, CORS_METHODS_DEFAULT);
    }

    public void start() {
        try {
            scan();
            log.info("Rest Handlers scan finished");
            applicationsClasses.entrySet().stream().forEach(appClass -> {
                try {
                    BaseApplication app = appClass.getValue().newInstance();
                    log.infof("Starting App [ %s ] - BaseUrl [ %s ]", appClass.getKey(), app.baseUrl());
                    app.start();
                    applications.put(appClass.getKey(), app);
                } catch (Exception e) {
                    log.errorf(e, "Error loading App [%s]", appClass);
                }
            });
            endpointsClasses.entrySet().stream().forEach(endpoint -> {
                try {
                    String endpointPackage = endpoint.getValue().getPackage().getName();

                    BaseApplication app = null;
                    while (endpointPackage.length() != 0) {
                        app = applications.get(endpointPackage);
                        if (app != null) {
                            break;
                        }

                        if (endpointPackage.lastIndexOf('.') == -1) {
                            break;
                        }

                        endpointPackage = endpointPackage.substring(0, endpointPackage.lastIndexOf('.'));
                    }

                    if (app == null) {
                        log.errorf("Handler [%s] does not belong to an application.", endpoint.getValue().getName());
                        return;
                    }

                    String baseUrl = app.baseUrl();
                    router.route(baseUrl + "*").handler(BodyHandler.create());

                    if (corsAllowedOriginPattern.length() > 0) {
                        CorsHandler corsHandler = CorsHandler.create(corsAllowedOriginPattern);
                        if (corsHeaders.length() > 0) {
                            corsHandler.allowedHeaders(extractCorsHeaders(corsHeaders));
                        }
                        if (corsMethods.length() > 0) {
                            corsHandler.allowedMethods(extractCorsMethods(corsMethods));
                        }
                        router.route(baseUrl + "*").handler(corsHandler);
                    }
                    RestHandler handler = endpoint.getValue().newInstance();
                    handler.initRoutes(baseUrl, router);
                    endpoints.put(endpoint.getKey(), handler);
                    log.infof("Starting on [ %s ]: Endpoint [ %s ] - Handler [ %s ]", baseUrl, endpoint.getKey().getEndpoint(), endpoint.getValue().getName());
                } catch (Exception e) {
                    log.errorf(e, "Error loading Handler [%s]", endpoint);
                }
            });
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    public void handle(HttpServerRequest req) {
        log.debugf("%s %s %s", req.method().name(), req.path(), req.params());
        router.accept(req);
    }

    public void stop() {
        applications.entrySet().stream().forEach(application -> application.getValue().stop());
    }

    private void scan() throws IOException {
        List<String> pathsToProcess = new ArrayList<>();

        ClassLoader classLoader = getClass().getClassLoader();
        URL[] classLoaderPaths = ((URLClassLoader) classLoader).getURLs();
        for (int i = 0; i < classLoaderPaths.length; i++) {
            pathsToProcess.add(classLoaderPaths[i].getPath());
        }

        String[] classpath = System.getProperty("java.class.path").split(":");
        for (int i = 0; i < classpath.length; i++) {
            pathsToProcess.add(classpath[i]);
        }

        for (String fullFileName : pathsToProcess) {
            File file = new File(fullFileName);
            String fileName = file.getName();
            if (file.isDirectory()) {
                processDirectory(fullFileName, fullFileName.length());
            } else if (fileName.contains("hawkular") && fileName.endsWith("jar")) {
                processArchive(fullFileName);
            }
        }
    }

    private void processArchive(String path) throws IOException, FileNotFoundException {
        log.debug(path);
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(path))) {
            for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                if (!entry.isDirectory() && entry.getName().endsWith(".class")
                        && entry.getName().contains("hawkular")) {
                    processClass(entry.getName());
                }
            }
        }
    }

    public void processDirectory(String directoryName, int rootLength) {
        log.debug(directoryName);
        File directory = new File(directoryName);
        for (File file : directory.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".class")) {
                processClass(file.getPath().substring(rootLength));
            } else if (file.isDirectory()) {
                processDirectory(file.getAbsolutePath(), rootLength);
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void processClass(String className) {
        className = className.replace('/', '.'); // including ".class"
        className = className.substring(0, className.length() - 6);
        log.debug(className);
        try {
            Class clazz = cl.loadClass(className);
            Class<?>[] interfaces = clazz.getInterfaces();
            for (int j = 0; j < interfaces.length; j++) {
                if (interfaces[j].equals(BaseApplication.class)) {
                    String appName = clazz.getPackage().getName();
                    applicationsClasses.put(appName, clazz);
                }
            }
            if (clazz.isAnnotationPresent(RestEndpoint.class)) {
                RestEndpoint endpoint = (RestEndpoint)clazz.getAnnotation(RestEndpoint.class);
                for (int j=0; j<interfaces.length; j++) {
                    if (interfaces[j].equals(RestHandler.class)) {
                        endpointsClasses.put(new EndpointKey(endpoint.path(), clazz.getCanonicalName()), clazz);
                    }
                }
            }
        } catch (Exception | Error e) {
            log.errorf(e, "Error loading Handler [%s].", className);
            System.exit(1);
        }
    }

    private Set<String> extractCorsHeaders(String corsHeaders) {
        Set<String> headers = new HashSet<>();
        if (corsHeaders != null && corsHeaders.length() > 0) {
            String[] splitted = corsHeaders.split(",");
            for (int i = 0; i < splitted.length; i++) {
                headers.add(splitted[i]);
            }
        }
        return headers;
    }

    private Set<HttpMethod> extractCorsMethods(String corsMethods) {
        Set<HttpMethod> methods = new HashSet<>();
        if (corsMethods != null && corsMethods.length() > 0) {
            String[] splitted = corsMethods.split(",");
            for (int i = 0; i < splitted.length; i++) {
                try {
                    methods.add(HttpMethod.valueOf(splitted[i]));
                } catch (Exception e) {
                    log.warnf("Skipping unknown CORS method [%s]: %s", splitted[i], e.getMessage());
                }
            }
        }
        return methods;
    }

    public static class EndpointKey {
        private String endpoint;
        private String className;

        public EndpointKey(String endpoint, String className) {
            this.endpoint = endpoint;
            this.className = className;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getClassName() {
            return className;
        }

        public void setClassName(String className) {
            this.className = className;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EndpointKey that = (EndpointKey) o;

            if (endpoint != null ? !endpoint.equals(that.endpoint) : that.endpoint != null) return false;
            return className != null ? className.equals(that.className) : that.className == null;
        }

        @Override
        public int hashCode() {
            int result = endpoint != null ? endpoint.hashCode() : 0;
            result = 31 * result + (className != null ? className.hashCode() : 0);
            return result;
        }
    }
}
