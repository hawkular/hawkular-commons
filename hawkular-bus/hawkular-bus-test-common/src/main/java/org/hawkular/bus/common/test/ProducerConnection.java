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
import javax.jms.Message;

import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.producer.ProducerConnectionContext;

/**
 * Convenience class tests can use to create a producer of either topic or queue
 * messages.
 *
 * The constructor creates the connection after which you just call sendMessage
 * to produce a message.
 */
public class ProducerConnection extends ConnectionContextFactory {

    private ProducerConnectionContext pcc;

    public ProducerConnection(String brokerURL, Endpoint endpoint) throws JMSException {
        super(brokerURL);
        prepareProducer(brokerURL, endpoint);
    }

    protected void prepareProducer(String brokerURL, Endpoint endpoint) throws JMSException {
        pcc = new ProducerConnectionContext();
        createConnection(pcc);
        cacheConnection(pcc.getConnection(), false);
        getConnection().start();
        createSession(pcc);
        createDestination(pcc, endpoint);
        pcc.setMessageProducer(pcc.getSession().createProducer(pcc.getDestination()));
    }

    public ProducerConnectionContext getConsumerConnectionContext() {
        return pcc;
    }

    public void sendMessage(String msg) throws JMSException {
        Message producerMessage = pcc.getSession().createTextMessage(msg);
        pcc.getMessageProducer().send(producerMessage);

    }
}
