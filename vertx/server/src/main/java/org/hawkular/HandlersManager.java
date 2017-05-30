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

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.hawkular.commons.log.MsgLogger;
import org.hawkular.commons.log.MsgLogging;
import org.hawkular.handlers.BaseApplication;
import org.hawkular.handlers.RestEndpoint;
import org.hawkular.handlers.RestHandler;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class HandlersManager {
    private static final MsgLogger log = MsgLogging.getMsgLogger(HandlersManager.class);
    private Router router;
    private Map<String, BaseApplication> applications = new HashMap<>();
    private Map<String, Class<BaseApplication>> applicationsClasses = new HashMap<>();
    private Map<String, RestHandler> endpoints = new HashMap<>();
    private Map<String, Class<RestHandler>> endpointsClasses = new HashMap<>();
    private ClassLoader cl = Thread.currentThread().getContextClassLoader();

    public HandlersManager(Vertx vertx) {
        this.router = Router.router(vertx);
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
                    log.infof("Starting Endpoint [ %s ] - Handler [ %s ]", endpoint.getKey(), endpoint.getValue().getName());
                    String endpointPackage = endpoint.getValue().getPackage().getName();
                    BaseApplication app = applications.get(endpointPackage);
                    if (app == null) {
                        log.errorf("Handler [%s] does not belong to an application.", endpoint.getValue().getName());
                        return;
                    }
                    String baseUrl = app.baseUrl();
                    router.route(baseUrl + "*").handler(BodyHandler.create());
                    RestHandler handler = endpoint.getValue().newInstance();
                    handler.initRoutes(baseUrl, router);
                    endpoints.put(endpoint.getKey(), handler);
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

    @SuppressWarnings("unchecked")
    private void scan() throws IOException {
        String[] classpath = System.getProperty("java.class.path").split(":");
        for (int i=0; i<classpath.length; i++) {
            if (classpath[i].contains("hawkular") && classpath[i].endsWith("jar")) {
                ZipInputStream zip = new ZipInputStream(new FileInputStream(classpath[i]));
                for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                        String className = entry.getName().replace('/', '.'); // including ".class"
                        className = className.substring(0, className.length() - 6);
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
                                        endpointsClasses.put(endpoint.path(), clazz);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.errorf(e, "Error loading Handler [%s].", className);
                            System.exit(1);
                        }
                    }
                }
            }
        }
    }
}
