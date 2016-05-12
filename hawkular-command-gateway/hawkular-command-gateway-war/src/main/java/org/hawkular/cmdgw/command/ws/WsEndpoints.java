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
import javax.enterprise.context.Destroyed;
import javax.enterprise.event.Observes;

import org.hawkular.cmdgw.command.ws.server.FeedWebSocket;
import org.hawkular.cmdgw.command.ws.server.UIClientWebSocket;
import org.hawkular.cmdgw.log.GatewayLoggers;
import org.hawkular.cmdgw.log.MsgLogger;

/**
 * A container class for {@link #feedSessions} and {@link #uiClientSessions} so that one does not need to inject them
 * separately as in most cases they are used together.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@ApplicationScoped
public class WsEndpoints {
    private static final MsgLogger log = GatewayLoggers.getLogger(WsEndpoints.class);

    private final WsSessions feedSessions;
    private final WsSessions uiClientSessions;

    public WsEndpoints() {
        super();
        this.uiClientSessions = new WsSessions(UIClientWebSocket.ENDPOINT);
        this.feedSessions = new WsSessions(FeedWebSocket.ENDPOINT);
    }

    public WsSessions getFeedSessions() {
        return feedSessions;
    }

    public WsSessions getUiClientSessions() {
        return uiClientSessions;
    }

    public void destroy(@Observes @Destroyed(ApplicationScoped.class) Object ignore) {
        log.debugf("Destroying [%s]", this.getClass().getName());
        this.uiClientSessions.destroy();
        this.feedSessions.destroy();
    }

}
