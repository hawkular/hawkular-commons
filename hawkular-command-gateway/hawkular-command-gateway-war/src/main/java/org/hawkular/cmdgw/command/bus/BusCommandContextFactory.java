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
package org.hawkular.cmdgw.command.bus;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.hawkular.bus.common.Endpoint;
import org.hawkular.cmdgw.command.ws.WsEndpoints;

/**
 * A factory for creation of {@link BusCommandContext}s.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@ApplicationScoped
public class BusCommandContextFactory {
    @Inject
    private WsEndpoints wsEndpoints;

    @Inject
    private BusConnectionFactoryProvider connectionFactoryProvider;

    /**
     * Creates a new {@link BusCommandContext} with the given {@code endpoint}.
     *
     * @param endpoint the queue or topic the present request came from
     * @return a new {@link BusCommandContext}
     */
    public BusCommandContext newCommandContext(Endpoint endpoint) {
        return new BusCommandContext(endpoint, connectionFactoryProvider.getConnectionFactory(),
                wsEndpoints.getUiClientSessions(), wsEndpoints.getFeedSessions());
    }

}
