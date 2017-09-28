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

import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.MessageId;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.producer.ProducerConnectionContext;
import org.hawkular.cmdgw.Constants;
import org.hawkular.cmdgw.api.EventDestination;
import org.hawkular.cmdgw.log.GatewayLoggers;
import org.hawkular.cmdgw.log.MsgLogger;

/**
 * A {@link WsCommand} that transfers messages implementing {@link EventDestination} to the events
 * subsystem.
 * <p>
 * This particular command implementation always puts the message on the
 * {@link Constants#EVENTS_COMMAND_QUEUE} bus endpoint.
 */
public class EventDestinationWsCommand implements WsCommand<EventDestination> {
    private static final MsgLogger log = GatewayLoggers.getLogger(EventDestinationWsCommand.class);

    @Override
    public void execute(BasicMessageWithExtraData<EventDestination> message, WsCommandContext context)
            throws Exception {
        EventDestination request = message.getBasicMessage();
        try (ConnectionContextFactory ccf = new ConnectionContextFactory(context.getConnectionFactory())) {

            ProducerConnectionContext pcc = ccf.createProducerConnectionContext(Constants.EVENTS_COMMAND_QUEUE);
            MessageId mid = new MessageProcessor().send(pcc, request);
            log.debugf("Event forwarded to bus: mid=[%s], request=[%s]", mid, request);
        }
    }
}
