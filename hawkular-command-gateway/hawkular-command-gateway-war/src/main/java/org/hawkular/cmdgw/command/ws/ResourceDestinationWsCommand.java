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
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageId;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.producer.ProducerConnectionContext;
import org.hawkular.cmdgw.Constants;
import org.hawkular.cmdgw.api.GenericSuccessResponse;
import org.hawkular.cmdgw.api.ResourceDestination;
import org.hawkular.cmdgw.command.ws.server.WebSocketHelper;
import org.hawkular.cmdgw.log.GatewayLoggers;
import org.hawkular.cmdgw.log.MsgLogger;

/**
 * A {@link WsCommand} that transfers messages implementing {@link ResourceDestination} from a UI WebSocket to a bus
 * endpoint to be handled by a feed. The feedId is extracted from {@link ResourceDestination#getFeedId()}.
 * <p>
 * This particular command implementation always puts the message on the
 * {@link Constants#FEED_COMMAND_QUEUE} bus endpoint.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class ResourceDestinationWsCommand implements WsCommand<ResourceDestination> {
    private static final MsgLogger log = GatewayLoggers.getLogger(ResourceDestinationWsCommand.class);

    @Override
    public void execute(BasicMessageWithExtraData<ResourceDestination> message, WsCommandContext context)
            throws Exception {
        ResourceDestination request = message.getBasicMessage();
        String feedId = request.getFeedId();
        String resourceId = request.getResourceId();
        if (feedId == null || resourceId == null) {
            throw new IllegalStateException(request.getClass().getName() + ".{feedId, resourceId} must not be null");
        }

        log.tracef("[%s] is about to execute the request [%s] ", getClass().getName(), request);
        // determine what feed needs to be sent the message

        try (ConnectionContextFactory ccf = new ConnectionContextFactory(context.getConnectionFactory())) {
            Endpoint endpoint = Constants.FEED_COMMAND_QUEUE;
            ProducerConnectionContext pcc = ccf.createProducerConnectionContext(endpoint);

            MessageId mid = new MessageProcessor().send(pcc, message,
                    Collections.singletonMap(Constants.HEADER_FEEDID, feedId));
            log.debugf("Message [%s] forwarded to bus endpoint [%s]", request.getClass().getName(),
                    endpoint.getName());
            GenericSuccessResponse response = new GenericSuccessResponse();
            response.setMessage("The request has been forwarded to feed [" + feedId + "] (MessageId=" + mid + ")");
            BasicMessageWithExtraData<GenericSuccessResponse> result = new BasicMessageWithExtraData<>(response, null);
            new WebSocketHelper().sendSync(context.getSession(), result);
        }
    }
}
