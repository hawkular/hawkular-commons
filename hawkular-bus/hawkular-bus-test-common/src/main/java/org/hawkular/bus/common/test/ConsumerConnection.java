/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.bus.common.test;

import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;

import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.consumer.ConsumerConnectionContext;

/**
 * Convenience class tests can use to create a consumer of either topic or queue
 * messages from a broker.
 *
 * The constructor creates the connection and attaches the listener after which
 * the listener can start consuming messages as they are produced.
 */
public class ConsumerConnection extends ConnectionContextFactory {

    private ConsumerConnectionContext ccc;

    public ConsumerConnection(String brokerURL, Endpoint endpoint, MessageListener messageListener)
            throws JMSException {
        super(brokerURL);
        prepareConsumer(brokerURL, endpoint, messageListener);
    }

    protected void prepareConsumer(String brokerURL, Endpoint endpoint, MessageListener messageListener)
            throws JMSException {
        ccc = new ConsumerConnectionContext();
        createConnection(ccc);
        cacheConnection(ccc.getConnection(), false);
        getConnection().start();
        createSession(ccc);
        createDestination(ccc, endpoint);
        MessageConsumer consumer = ccc.getSession().createConsumer(ccc.getDestination());
        consumer.setMessageListener(messageListener);
    }

    public ConsumerConnectionContext getConsumerConnectionContext() {
        return ccc;
    }
}
