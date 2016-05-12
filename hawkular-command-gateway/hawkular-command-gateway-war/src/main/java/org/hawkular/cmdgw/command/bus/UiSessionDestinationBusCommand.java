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

import javax.websocket.Session;

import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.hawkular.cmdgw.api.UiSessionDestination;
import org.hawkular.cmdgw.command.ws.server.WebSocketHelper;
import org.hawkular.cmdgw.log.GatewayLoggers;
import org.hawkular.cmdgw.log.MsgLogger;

/**
 * A {@link BusCommand} that transfers messages implementing {@link UiSessionDestination} from a bus endpoint to a
 * WebSocket of a UI client.
 */
public class UiSessionDestinationBusCommand implements BusCommand<UiSessionDestination> {
    private static final MsgLogger log = GatewayLoggers.getLogger(UiSessionDestinationBusCommand.class);

    /**
     * This simply takes the given {@code message} and sends it directly to the UI client over
     * that UI client's websocket connection.
     *
     * @see org.hawkular.cmdgw.command.bus.BusCommand#execute(org.hawkular.bus.common.BasicMessageWithExtraData,
     *      org.hawkular.cmdgw.command.bus.BusCommandContext)
     */
    @Override
    public void execute(BasicMessageWithExtraData<UiSessionDestination> message, BusCommandContext context)
            throws Exception {
        UiSessionDestination request = message.getBasicMessage();
        String destinationSessionId = request.getDestinationSessionId();
        if (destinationSessionId == null) {
            throw new IllegalStateException(request.getClass().getName() + ".destinationSessionId must not be null");
        }

        log.tracef("[%s] is about to execute the request [%s] ", getClass().getName(), request);

        Session session = context.getConnectedUIClients().getSession(destinationSessionId);
        if (session != null) {
            new WebSocketHelper().sendSync(session, message);
        } else {
            throw new Exception("No such sessionId [" + destinationSessionId + "]");
        }
    }
}
