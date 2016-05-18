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
package org.hawkular.cmdgw;

import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.Endpoint.Type;

/**
 * Global constants.
 */
public interface Constants {
    /**
     * A JMS message header that will identify the targeted feed.
     */
    String HEADER_FEEDID = "feedId";

    /**
     * A JMS message header that will identify the targeted UI client.
     */
    String HEADER_UICLIENTID = "uiClientId";

    /**
     * The JNDI name of the bus connection factory.
     */
    String CONNECTION_FACTORY_JNDI = "java:/HawkularBusConnectionFactory";

    // QUEUES AND TOPICS
    Endpoint FEED_COMMAND_QUEUE = new Endpoint(Type.QUEUE, "FeedCommandQueue");
    Endpoint UI_COMMAND_QUEUE = new Endpoint(Type.QUEUE, "UiCommandQueue");

    Endpoint EVENTS_COMMAND_TOPIC = new Endpoint(Type.TOPIC, "HawkularCommandEvent");

    String CONNECTION_FACTORY_JNDI_LOOKUP_TIMEOUT_MS = "hawkular.cmdgw.connectionFactoryLookupTimeoutMs";
    int CONNECTION_FACTORY_JNDI_LOOKUP_TIMEOUT_MS_DEFAULT = 30000;
    String CONNECTION_FACTORY_JNDI_LOOKUP_RETRY_AFTER_MS = "hawkular.cmdgw.connectionFactoryLookupRetryAfterMs";
    int CONNECTION_FACTORY_JNDI_LOOKUP_RETRY_AFTER_MS_DEFAULT = 250;
}
