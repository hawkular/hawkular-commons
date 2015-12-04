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

import javax.jms.ConnectionFactory;

import org.hawkular.bus.common.AbstractMessage;
import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.consumer.ConsumerConnectionContext;
import org.hawkular.bus.common.consumer.RPCBasicMessageListener;
import org.jboss.logging.Logger;

public abstract class RPCBasicMessageDrivenBean<T extends AbstractMessage, U extends AbstractMessage> extends
        RPCBasicMessageListener<T, U> {
    private final Logger log = Logger.getLogger(RPCBasicMessageDrivenBean.class);

    /**
     * MDB subclasses need to define this usually by returning a factory that is obtained through injection:
     *
     * <pre>
     * &#064;Resource(mappedName = &quot;java:/HawkularBusConnectionFactory&quot;)
     * private ConnectionFactory connectionFactory;
     * </pre>
     *
     * @return connection factory to be used when sending the response
     */
    public abstract ConnectionFactory getConnectionFactory();

    @Override
    public ConsumerConnectionContext getConsumerConnectionContext() {
        ConsumerConnectionContext ctx = null;
        try {
            // here we build a faux consumer connection context whose data will be duplicated in
            // a producer connection context so the response message can be sent. The endpoint we
            // use here is just a dummy one - it will be replaced with the JMS ReplyTo by the superclass.
            ConnectionContextFactory ccf = new ConnectionContextFactory(getConnectionFactory());
            ctx = ccf.createConsumerConnectionContext(Endpoint.TEMPORARY_QUEUE);
        } catch (Exception e) {
            log.error("Failed to build context - will not be able to respond to message", e);
        }
        return ctx;
    }
}