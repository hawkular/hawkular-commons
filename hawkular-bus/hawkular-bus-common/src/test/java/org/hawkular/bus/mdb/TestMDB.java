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
package org.hawkular.bus.mdb;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.jms.Destination;
import javax.jms.MessageListener;

import org.hawkular.bus.common.BasicTextMessage;
import org.hawkular.bus.common.Bus;
import org.hawkular.bus.common.consumer.BasicMessageListener;
import org.jboss.logging.Logger;

/**
 * @author jsanda
 */
@MessageDriven(messageListenerInterface = MessageListener.class, activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "TestQueue")})
@TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)
public class TestMDB extends BasicMessageListener<BasicTextMessage> {

    private static final Logger log = Logger.getLogger(TestMDB.class);

    @Inject
    private Bus bus;

    @Override
    public void onBasicMessage(BasicTextMessage basicMessage) {
        try {
            log.debug("Received message");
            Destination replyTo = basicMessage.getReplyTo();
            bus.send(replyTo, new BasicTextMessage(basicMessage.getText().toUpperCase()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
