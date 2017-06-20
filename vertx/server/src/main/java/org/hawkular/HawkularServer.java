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

import java.lang.management.ManagementFactory;

import javax.management.ObjectName;

import org.hawkular.commons.log.MsgLogger;
import org.hawkular.commons.log.MsgLogging;
import org.hawkular.commons.properties.HawkularProperties;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;

/**
 * Lightweight HawkularServer based on Vert.X/Netty.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class HawkularServer implements HawkularServerMBean {
    private static final MsgLogger log = MsgLogging.getMsgLogger(HawkularServer.class);

    private static final String BIND_ADDRESS = "hawkular.bind-address";
    private static final String BIND_ADDRESS_DEFAULT = "127.0.0.1";
    private static final String PORT = "hawkular.port";
    private static final String PORT_DEFAULT = "8080";
    private static final String JMX_NAME = "org.hawkular:name=HawkularServer";

    private Vertx vertx;
    private HttpServer server;
    private HandlersManager handlers;

    public void start() {
        long start = System.currentTimeMillis();
        String bindAdress = HawkularProperties.getProperty(BIND_ADDRESS, BIND_ADDRESS_DEFAULT);
        Integer port = Integer.valueOf(HawkularProperties.getProperty(PORT, PORT_DEFAULT));

        try {
            vertx = Vertx.vertx();

            handlers = new HandlersManager(vertx);
            handlers.start();

            HttpServerOptions serverOptions = new HttpServerOptions();
            serverOptions.setCompressionSupported(true);
            server = vertx.createHttpServer(serverOptions);

            log.infof("Starting Server at http://%s:%s in [%s ms] ", bindAdress, port, (System.currentTimeMillis() - start));
            server.requestHandler(handlers::handle).listen(port, bindAdress);
        } catch (Exception e) {
            log.error(e);
            log.error("Forcing exit");
            handlers.stop();
            server.close();
            System.exit(1);
        }
    }

    public String getStatus() {
        return server != null ? "STARTED" : "STOPPED";
    }

    public void stop() {
        log.info("Stopping Server");
        handlers.stop();
        server.close();
        log.info("Server stopped");
        System.exit(0);
    }

    public static void registerMBean(HawkularServer server) {
        try {
            ObjectName jmxName = new ObjectName(JMX_NAME);
            ManagementFactory.getPlatformMBeanServer().registerMBean(server, jmxName);
        } catch (Exception exception) {
            log.error("Unable to register JMX Bean");
        }
    }

    public static void main(String[] args) {
        HawkularServer server = new HawkularServer();
        registerMBean(server);
        server.start();
    }
}
