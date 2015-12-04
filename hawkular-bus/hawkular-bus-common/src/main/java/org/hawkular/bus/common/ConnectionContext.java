/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.bus.common;

import java.io.Closeable;
import java.io.IOException;

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;

/**
 * This is a simple POJO that just contains objects related to particular connection. This object does not distinguish
 * between a producer's connection or consumer's connection - that is the job of the subclasses.
 */
public class ConnectionContext implements Closeable {
    private Connection connection;
    private Session session;
    private Destination destination;

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public Destination getDestination() {
        return destination;
    }

    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    /**
     * Sets this context object with the same data found in the source context.
     *
     * @param source the source context whose data is to be copied
     */
    public void copy(ConnectionContext source) {
        this.connection = source.connection;
        this.session = source.session;
        this.destination = source.destination;
    }

    @Override
    public void close() throws IOException {
        if (session != null) {
            try {
                session.close();
            } catch (JMSException e) {
                throw new IOException(e);
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException e) {
                throw new IOException(e);
            }
        }
    }
}
