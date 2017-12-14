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

import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import org.hawkular.cmdgw.log.GatewayLoggers;
import org.hawkular.cmdgw.log.MsgLogger;

@ServerEndpoint(FeedWebSocket.ENDPOINT)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class FeedWebSocket extends AbstractGatewayWebSocket {
    public static final String ENDPOINT = "/feed/{feedId}";
    private static MsgLogger log = GatewayLoggers.getLogger(FeedWebSocket.class);

    public FeedWebSocket() {
        super(ENDPOINT);
    }

    @OnOpen
    public void feedSessionOpen(Session session, @PathParam("feedId") String feedId) {
        log.infoWsSessionOpened(feedId, endpoint);
        wsEndpoints.getFeedSessions().addSession(feedId, session);
    }

    @OnClose
    public void feedSessionClose(Session session, CloseReason reason, @PathParam("feedId") String feedId) {
        log.infoWsSessionClosed(feedId, endpoint, reason);
        wsEndpoints.getFeedSessions().removeSession(feedId, session);
    }

}
