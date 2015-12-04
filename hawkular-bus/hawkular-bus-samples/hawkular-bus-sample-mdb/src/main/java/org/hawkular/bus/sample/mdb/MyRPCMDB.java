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
package org.hawkular.bus.sample.mdb;

import javax.annotation.Resource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.jms.ConnectionFactory;
import javax.jms.MessageListener;

import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.hawkular.bus.common.SimpleBasicMessage;
import org.hawkular.bus.mdb.RPCBasicMessageDrivenBean;
import org.jboss.logging.Logger;

@MessageDriven(messageListenerInterface = MessageListener.class, activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
        @ActivationConfigProperty(propertyName = "destination", propertyValue = "ExampleQueueName"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "MyFilter = 'rpc'") })
public class MyRPCMDB extends RPCBasicMessageDrivenBean<SimpleBasicMessage, SimpleBasicMessage> {
    private final Logger log = Logger.getLogger(MyRPCMDB.class);

    @Resource(mappedName = "java:/HawkularBusConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Override
    public ConnectionFactory getConnectionFactory() {
        return this.connectionFactory;
    }

    @Override
    protected SimpleBasicMessage onBasicMessage(BasicMessageWithExtraData<SimpleBasicMessage> msgWithExtraData) {
        SimpleBasicMessage msg = msgWithExtraData.getBasicMessage();
        log.infof("===> MDB received incoming RPC message [%s]", msg);
        SimpleBasicMessage response = new SimpleBasicMessage("ECHO! " + msg.getMessage());
        log.infof("===> MDB sending response RPC message [%s]", response);
        return response;
    }
}
