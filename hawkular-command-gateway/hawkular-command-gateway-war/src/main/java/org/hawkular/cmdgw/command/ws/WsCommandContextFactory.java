/*
 * Copyright 2014-2016 Red Hat, Inc. and/or its affiliates
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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.websocket.Session;

import org.hawkular.cmdgw.command.bus.BusConnectionFactoryProvider;

/**
 * A factory for creatiion of {@link WsCommandContext}s.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@ApplicationScoped
public class WsCommandContextFactory {
    @Inject
    private WsEndpoints wsEndpoints;

    @Inject
    private BusConnectionFactoryProvider connectionFactoryProvider;

    /**
     * Creates a new {@link WsCommandContext} with the given {@code session}.
     *
     * @param session the session the present request came from
     * @return a new {@link WsCommandContext}
     */
    public WsCommandContext newCommandContext(Session session) {
        return new WsCommandContext(connectionFactoryProvider.getConnectionFactory(), session,
                wsEndpoints.getUiClientSessions(), wsEndpoints.getFeedSessions());
    }

}
