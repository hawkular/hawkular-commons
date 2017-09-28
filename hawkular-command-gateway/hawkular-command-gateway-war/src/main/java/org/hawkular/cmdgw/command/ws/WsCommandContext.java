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
package org.hawkular.cmdgw.command.ws;

import javax.jms.ConnectionFactory;
import javax.websocket.Session;

/**
 * Context information that may be useful during an execution of a {@link WsCommand}.
 *
 * @see WsCommand#execute(org.hawkular.bus.common.BasicMessageWithExtraData, WsCommandContext)
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class WsCommandContext {

    private final ConnectionFactory connectionFactory;
    private final Session session;
    private final WsSessions connectedUIClients;
    private final WsSessions connectedFeeds;

    public WsCommandContext(ConnectionFactory connectionFactory, Session session, WsSessions connectedUIClients,
            WsSessions connectedFeeds) {
        super();
        this.connectionFactory = connectionFactory;
        this.session = session;
        this.connectedUIClients = connectedUIClients;
        this.connectedFeeds = connectedFeeds;
    }

    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * @return the {@link Session} the present request came from
     */
    public Session getSession() {
        return session;
    }

    public WsSessions getConnectedUIClients() {
        return connectedUIClients;
    }

    public WsSessions getConnectedFeeds() {
        return connectedFeeds;
    }
}