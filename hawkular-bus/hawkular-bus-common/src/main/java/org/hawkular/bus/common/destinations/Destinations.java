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
package org.hawkular.bus.common.destinations;

import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.Topic;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author jsanda
 */
@Dependent
public class Destinations {

    private InitialContext context;

    @PostConstruct
    public void init() {
        try {
            context = new InitialContext();
            System.out.println("JMS BINDINGS " + context.listBindings("java:/jms"));
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    @Produces
    @JMSQueue("")
    public Queue getQueue(InjectionPoint injectionPoint) {
        JMSQueue jmsQueue = injectionPoint.getAnnotated().getAnnotation(JMSQueue.class);
        return getDestination("queue", jmsQueue.value());
    }

    @Produces
    @JMSTopic("")
    public Topic getTopic(InjectionPoint injectionPoint) {
        JMSTopic jmsTopic = injectionPoint.getAnnotated().getAnnotation(JMSTopic.class);
        return getDestination("topic", jmsTopic.value());
    }

    @SuppressWarnings("unchecked")
    private <T extends Destination> T getDestination(String type, String name) {
        try {
            return (T) context.lookup("java:/jms/" + type + "/" + name);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
}
