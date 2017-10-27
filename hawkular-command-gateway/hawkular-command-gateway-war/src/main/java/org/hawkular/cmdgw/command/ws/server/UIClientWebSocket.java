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
package org.hawkular.cmdgw.command.ws.server;

import java.io.IOException;

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.hawkular.cmdgw.api.WelcomeResponse;
import org.hawkular.cmdgw.log.GatewayLoggers;
import org.hawkular.cmdgw.log.MsgLogger;

/**
 * This is similiar to the feed web socket endpoint, however, it has a different set of allowed commands that can be
 * processed for a UI client.
 */
@ServerEndpoint(UIClientWebSocket.ENDPOINT)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class UIClientWebSocket extends AbstractGatewayWebSocket {
    public static final String ENDPOINT = "/ui/ws";
    static final MsgLogger log = GatewayLoggers.getLogger(UIClientWebSocket.class);

    public UIClientWebSocket() {
        super(ENDPOINT);
    }

    /**
     * When a UI client connects, this method is called. This will immediately send a welcome
     * message to the UI client.
     *
     * @param session the new UI client's session
     */
    @OnOpen
    public void uiClientSessionOpen(Session session) {
        log.infoWsSessionOpened(session.getId(), endpoint);
        wsEndpoints.getUiClientSessions().addSession(session.getId(), session);
        WelcomeResponse welcomeResponse = new WelcomeResponse();
        // FIXME we should not send the true sessionIds to clients to prevent spoofing.
        welcomeResponse.setSessionId(session.getId());
        try {
            new WebSocketHelper().sendBasicMessageSync(session, welcomeResponse);
        } catch (IOException e) {
            log.warnf(e, "Could not send [%s] to UI client session [%s].", WelcomeResponse.class.getName(),
                    session.getId());
        }
    }

    @OnClose
    public void uiClientSessionClose(Session session, CloseReason reason) {
        log.infoWsSessionClosed(session.getId(), endpoint, reason);
        wsEndpoints.getUiClientSessions().removeSession(session.getId(), session);
    }

}
