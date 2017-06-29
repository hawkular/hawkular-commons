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

import java.rmi.UnmarshalException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class HawkularManager {
    static final String DEFAULT_HOST = "localhost";
    static final String DEFAULT_PORT = "9080";
    static final String JMX_NAME = "org.hawkular:name=HawkularServer";

    static final String STOP = "stop";
    static final String STATUS = "status";

    static JMXConnector jmxConn;
    static MBeanServerConnection server;
    static ObjectName hwkMBean;

    static void connect(String host, String port) {
        try {
            String url = "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi";
            JMXServiceURL jmxUrl = new JMXServiceURL(url);
            jmxConn = JMXConnectorFactory.connect(jmxUrl, null);
            server = jmxConn.getMBeanServerConnection();
            hwkMBean = new ObjectName(JMX_NAME);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    static void disconnect() {
        try {
            if (jmxConn != null) {
                jmxConn.close();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    static void status() {
        try {
            if (server != null) {
                Object status = server.getAttribute(hwkMBean, "Status");
                System.out.println(status);
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    static void stop() {
        try {
            if (server != null) {
                server.invoke(hwkMBean, "stop", null, null);
            }
        } catch (UnmarshalException e) {
            // Expected as remote process will stop
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    static void printUsage() {
        System.out.println("Usage: org.hawkular.HawkularManager OPERATION [HOST] [PORT]");
        System.out.println("Invoke a remote HawkularServer MBean\n");
        System.out.println("OPERATION:\n");
        System.out.println("    status      Get current status of a HawkularServer");
        System.out.println("    stop        Stop a HawkularServer\n");
        System.out.println("HOST [Optional]:\n");
        System.out.println("    Define host of the HawkularServer, default is localhost");
        System.out.println("PORT [Optional]:\n");
        System.out.println("    Define port of the HawkularServer, default is 9080");
    }

    public static void main(String[] args) {
        String operation = "";
        String host = DEFAULT_HOST;
        String port = DEFAULT_PORT;
        if (args != null) {
            if (args.length < 1 || args.length > 3) {
                printUsage();
                System.exit(1);
            }
            operation = args[0];
            if (!operation.equals(STOP) && !operation.equals(STATUS)) {
                printUsage();
                System.exit(1);
            }
            if (args.length > 1) {
                host = args[1];
            }
            if (args.length > 2) {
                port = args[2];
            }
        }
        connect(host, port);
        if (operation.equals(STATUS)) {
            status();
            disconnect();
        }
        if (operation.equals(STOP)) {
            stop();
        }
    }
}
