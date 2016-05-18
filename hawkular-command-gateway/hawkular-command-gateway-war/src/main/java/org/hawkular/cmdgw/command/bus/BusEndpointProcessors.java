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

import java.io.IOException;
import java.util.function.BiFunction;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.websocket.Session;

import org.hawkular.bus.common.BasicMessage;
import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.consumer.BasicMessageListener;
import org.hawkular.bus.common.consumer.ConsumerConnectionContext;
import org.hawkular.cmdgw.Constants;
import org.hawkular.cmdgw.command.ws.WsEndpoints;
import org.hawkular.cmdgw.command.ws.WsSessionListener;
import org.hawkular.cmdgw.command.ws.server.WebSocketHelper;
import org.hawkular.cmdgw.log.GatewayLoggers;
import org.hawkular.cmdgw.log.MsgLogger;

/**
 * A collection of listeners that are attached/removed to/from bus queues or topics as WebSocket clients connect and
 * disconnect.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@ApplicationScoped
public class BusEndpointProcessors {

    /**
     * A {@link WsSessionListener} that adds the given {@link #busEndpointListener} to the given {@link #endpoint} on
     * {@link #sessionAdded()} and removes it on {@link #sessionRemoved()}.
     * <p>
     * Each instance of this object is responsible for adding a bus listener to a specific bus queue or topic that
     * will listen only for messages for a specific client (see the "selectorHeader" and "selectorValue"
     * arguments to the constructor).
     */
    private class BusWsSessionListener implements WsSessionListener {
        private final BasicMessageListener<BasicMessage> busEndpointListener;
        private ConsumerConnectionContext consumerConnectionContext;

        private final Endpoint endpoint;
        private final String messageSelector;
        private ConnectionContextFactory connectionContextFactory;

        public BusWsSessionListener(String selectorHeader, String selectorValue, Endpoint endpoint,
                BasicMessageListener<BasicMessage> busEndpointListener) {
            super();

            this.endpoint = endpoint;
            this.busEndpointListener = busEndpointListener;
            this.messageSelector = String.format("%s = '%s'", selectorHeader, selectorValue);

            log.debugf("Created [%s] for messageSelector [%s]", getClass().getName(), messageSelector);
        }

        /**
         * Adds the given {@link #busEndpointListener} to the given {@link #endpoint}.
         *
         * @see org.hawkular.cmdgw.command.ws.WsSessionListener#sessionAdded()
         */
        @Override
        public void sessionAdded() {
            log.debugf("Attaching [%s] with selector [%s] to bus endpoint [%s]",
                    busEndpointListener.getClass().getName(), messageSelector, endpoint);

            try {
                connectionContextFactory =
                        new ConnectionContextFactory(true, connectionFactoryProvider.getConnectionFactory());
                consumerConnectionContext = connectionContextFactory.createConsumerConnectionContext(endpoint,
                        messageSelector);
                new MessageProcessor().listen(consumerConnectionContext, busEndpointListener);
            } catch (JMSException e) {
                log.errorCouldNotAddBusEndpointListener(busEndpointListener.getClass().getName(), messageSelector,
                        endpoint.getName(), e);
            }
        }

        /**
         * Removes the given {@link #busEndpointListener} from the given {@link #endpoint}.
         *
         * @see org.hawkular.cmdgw.command.ws.WsSessionListener#sessionRemoved()
         */
        @Override
        public void sessionRemoved() {
            log.debugf("Removing [%s] with selector [%s] from bus endpoint [%s]",
                    busEndpointListener.getClass().getName(), messageSelector, endpoint);

            if (consumerConnectionContext != null) {
                try {
                    consumerConnectionContext.close();
                } catch (IOException e) {
                    log.errorCouldNotClose(consumerConnectionContext.getClass().getName(), messageSelector,
                            endpoint.getName(), e);
                }
            }

            if (connectionContextFactory != null) {
                try {
                    connectionContextFactory.close();
                } catch (Exception e) {
                    log.errorCouldNotCloseConnectionContextFactory(e, connectionContextFactory.getClass().getName());
                }
            }
        }

    }

    /**
     * This is the actual JMS bus listener that will consume bus messages destined for
     * a particular feed. When this listener receives a bus message, it will forward
     * that message to the feed over the feed's websocket connection.
     */
    private static class FeedBusEndpointListener extends BasicMessageListener<BasicMessage> {

        private final Endpoint endpoint;
        private final String expectedFeedId;
        private final Session session;

        public FeedBusEndpointListener(Session session, String expectedFeedId, Endpoint endpoint) {
            super(FeedBusEndpointListener.class.getClassLoader());
            this.session = session;
            this.expectedFeedId = expectedFeedId;
            this.endpoint = endpoint;
        }

        @Override
        protected void onBasicMessage(BasicMessageWithExtraData<BasicMessage> messageWithData) {
            final BasicMessage basicMessage = messageWithData.getBasicMessage();
            try {
                log.debugf("Received message [%s] with binary data [%b] from endpoint [%s]",
                        basicMessage.getClass().getName(), messageWithData.getBinaryData() != null,
                        endpoint.getName());
                String foundFeedId = basicMessage.getHeaders().get(Constants.HEADER_FEEDID);
                if (foundFeedId == null) {
                    log.errorMessageWithoutFeedId(basicMessage.getClass().getName(), Constants.HEADER_FEEDID,
                            endpoint.toString());
                } else if (!foundFeedId.equals(expectedFeedId)) {
                    log.errorListenerGotMessageWithUnexpectedHeaderValue(this.getClass().getName(),
                            basicMessage.getClass().getName(), Constants.HEADER_FEEDID, foundFeedId,
                            expectedFeedId, endpoint.toString());
                } else {
                    new WebSocketHelper().sendSync(session, messageWithData);
                }
            } catch (Exception e) {
                log.errorCouldNotProcessBusMessage(basicMessage.getClass().getName(),
                        messageWithData.getBinaryData() != null, endpoint.getName(), e);
            }
        }

    }

    /**
     * This is the actual JMS bus listener that will consume bus messages destined for
     * a particular UI client. When this listener receives command as a bus message,
     * that command's executor will be invoked. Typically, that invocation will
     * simply forward that command message to the UI client over the UI client's websocket
     * connection but command implementations can vary.
     */
    private static class UiClientBusEndpointListener
            extends org.hawkular.bus.common.consumer.BasicMessageListener<BasicMessage> {
        private final BusCommandContextFactory commandContextFactory;
        private final BusCommands commands;
        private final Endpoint endpoint;

        public UiClientBusEndpointListener(BusCommandContextFactory commandContextFactory, BusCommands commands,
                Endpoint endpoint) {
            super(UiClientBusEndpointListener.class.getClassLoader());
            this.commandContextFactory = commandContextFactory;
            this.commands = commands;
            this.endpoint = endpoint;
        }

        @Override
        protected void onBasicMessage(BasicMessageWithExtraData<BasicMessage> messageWithData) {
            final BasicMessage basicMessage = messageWithData.getBasicMessage();
            log.debugf("Received message [%s] with binary data [%b] from endpoint [%s]",
                    basicMessage.getClass().getName(), messageWithData.getBinaryData() != null,
                    endpoint.getName());
            try {
                @SuppressWarnings("unchecked")
                BusCommand<BasicMessage> command = (BusCommand<BasicMessage>) commands
                        .getCommand(messageWithData.getBasicMessage().getClass());
                BusCommandContext context = commandContextFactory.newCommandContext(null);
                command.execute(messageWithData, context);
            } catch (Exception e) {
                log.errorCouldNotProcessBusMessage(basicMessage.getClass().getName(),
                        messageWithData.getBinaryData() != null, endpoint.getName(), e);
            }
        }
    }

    private static final MsgLogger log = GatewayLoggers.getLogger(BusEndpointProcessors.class);

    @Inject
    private BusCommands busCommands;

    @Inject
    private BusCommandContextFactory commandContextFactory;

    /**
     * We might consider injecting an {@link Instance} of {@link ConnectionFactory} produced by
     * {@link BusConnectionFactoryProvider} here. See
     * https://github.com/hawkular/hawkular-commons/pull/65/files/4faa33502c68b6cd686a93fb3c0824e6574e0564#r63523505
     */
    @Inject
    private BusConnectionFactoryProvider connectionFactoryProvider;

    private BiFunction<String, Session, WsSessionListener> feedSessionListenerProducer;
    private BiFunction<String, Session, WsSessionListener> uiClientSessionListenerProducer;

    @Inject
    private WsEndpoints wsEndpoints;

    public void destroy(@Observes @Destroyed(ApplicationScoped.class) Object ignore) {
        log.debugf("Destroying [%s]", this.getClass().getName());
        if (feedSessionListenerProducer != null) {
            wsEndpoints.getFeedSessions().removeWsSessionListenerProducer(feedSessionListenerProducer);
        }
        if (uiClientSessionListenerProducer != null) {
            wsEndpoints.getUiClientSessions().removeWsSessionListenerProducer(uiClientSessionListenerProducer);
        }
    }

    /**
     * This creates the bi-function listener-producers that will create listeners which will
     * create JMS bus listeners for each websocket session that gets created in the future.
     *
     * @param ignore unused
     */
    public void initialize(@Observes @Initialized(ApplicationScoped.class) Object ignore) {
        log.debugf("Initializing [%s]", this.getClass().getName());
        try {
            feedSessionListenerProducer = new BiFunction<String, Session, WsSessionListener>() {
                @Override
                public WsSessionListener apply(String key, Session session) {
                    // In the future, if we need other queues/topics that need to be listened to, we add them here.
                    final Endpoint endpoint = Constants.FEED_COMMAND_QUEUE;
                    BasicMessageListener<BasicMessage> busEndpointListener = new FeedBusEndpointListener(session, key,
                            endpoint);
                    return new BusWsSessionListener(Constants.HEADER_FEEDID, key, endpoint, busEndpointListener);
                }
            };
            wsEndpoints.getFeedSessions().addWsSessionListenerProducer(feedSessionListenerProducer);

            uiClientSessionListenerProducer = new BiFunction<String, Session, WsSessionListener>() {
                @Override
                public WsSessionListener apply(String key, Session session) {
                    // In the future, if we need other queues/topics that need to be listened to, we add them here.
                    final Endpoint endpoint = Constants.UI_COMMAND_QUEUE;
                    BasicMessageListener<BasicMessage> busEndpointListener = new UiClientBusEndpointListener(
                            commandContextFactory, busCommands, endpoint);
                    return new BusWsSessionListener(Constants.HEADER_UICLIENTID, key, endpoint, busEndpointListener);
                }
            };
            wsEndpoints.getUiClientSessions().addWsSessionListenerProducer(uiClientSessionListenerProducer);
        } catch (Exception e) {
            log.errorCouldNotInitialize(e, this.getClass().getName());
        }

    }

}
