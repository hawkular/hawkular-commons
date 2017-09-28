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
package org.hawkular.cmdgw.command.bus;

import javax.jms.ConnectionFactory;

import org.hawkular.bus.common.Endpoint;
import org.hawkular.cmdgw.command.ws.WsSessions;

/**
 * Context information that may be useful during an execution of a {@link BusCommand}.
 *
 * @see BusCommand#execute(org.hawkular.bus.common.BasicMessageWithExtraData, BusCommandContext)
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class BusCommandContext {

    private final WsSessions connectedFeeds;
    private final WsSessions connectedUIClients;
    private final ConnectionFactory connectionFactory;
    private final Endpoint endpoint;

    public BusCommandContext(Endpoint endpoint, ConnectionFactory connectionFactory, WsSessions connectedUIClients,
            WsSessions connectedFeeds) {
        super();
        this.endpoint = endpoint;
        this.connectionFactory = connectionFactory;
        this.connectedUIClients = connectedUIClients;
        this.connectedFeeds = connectedFeeds;
    }

    public WsSessions getConnectedFeeds() {
        return connectedFeeds;
    }

    public WsSessions getConnectedUIClients() {
        return connectedUIClients;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * @return the Bus endpoint the present request came from
     */
    public Endpoint getEndpoint() {
        return endpoint;
    }
}