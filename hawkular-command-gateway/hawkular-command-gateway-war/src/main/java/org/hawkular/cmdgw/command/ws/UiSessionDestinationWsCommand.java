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

import java.util.Collections;

import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.MessageId;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.producer.ProducerConnectionContext;
import org.hawkular.cmdgw.Constants;
import org.hawkular.cmdgw.api.UiSessionDestination;
import org.hawkular.cmdgw.log.GatewayLoggers;
import org.hawkular.cmdgw.log.MsgLogger;

/**
 * A {@link WsCommand} that transfers messages implementing {@link UiSessionDestination} from a feed WebSocket to a bus
 * endpoint to be delivered to a UI WebSocket.
 * <p>
 * This particular command implementation always puts the message on the
 * {@link Constants#UI_COMMAND_QUEUE} bus endpoint.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class UiSessionDestinationWsCommand implements WsCommand<UiSessionDestination> {
    private static final MsgLogger log = GatewayLoggers.getLogger(UiSessionDestinationWsCommand.class);

    @Override
    public void execute(BasicMessageWithExtraData<UiSessionDestination> message, WsCommandContext context)
            throws Exception {
        UiSessionDestination request = message.getBasicMessage();
        try (ConnectionContextFactory ccf = new ConnectionContextFactory(context.getConnectionFactory())) {

            ProducerConnectionContext pcc = ccf.createProducerConnectionContext(Constants.UI_COMMAND_QUEUE);
            MessageId mid = new MessageProcessor().send(pcc, message,
                    Collections.singletonMap(Constants.HEADER_UICLIENTID, request.getDestinationSessionId()));
            log.debugf("Request forwarded to WebSocket. mid=[%s], request=[%s]", mid, request);
        }
    }
}
