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
package org.hawkular.bus.common;

import java.util.Objects;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;

import org.hawkular.bus.common.consumer.BasicMessageListener;

/**
 * A registration is creating when you call {@link Bus#register(Destination, BasicMessageListener)}. It stores details
 * about the listener that is used by the bus internally. Clients can use the registration's {@link #cancel() cancel}
 * method to stop listening for messages. Note however that a registration is automatically canceled when the bus that
 * created it is removed from the container.
 *
 * @author jsanda
 */
public class Registration {

    private Bus bus;

    private String destinationName;

    private String listenerName;

    private String messageSelector;

    <T extends BasicMessage> Registration(Bus bus, Destination destination, BasicMessageListener<T> listener)
            throws JMSException {
        this(bus, destination, listener, null);
    }

    <T extends BasicMessage> Registration(Bus bus, Destination destination, BasicMessageListener<T> listener,
            String messageSelector) throws JMSException {
        this.bus = bus;
        if (destination instanceof Queue) {
            this.destinationName = ((Queue) destination).getQueueName();
        } else {
            this.destinationName = ((Topic) destination).getTopicName();
        }
        this.listenerName = listener.getClass().getName();
        this.messageSelector = messageSelector;
    }

    /**
     * Causes the listener to stop listening for messages. This also closes the underling
     * {@link javax.jms.JMSConsumer JMSConsumer} and {@link javax.jms.JMSContext JMSContext}.
     */
    public void cancel() {
        bus.cancel(this);
    }

    @Override
    public String toString() {
        return "Registration{" +
                "destinationName='" + destinationName + '\'' +
                ", listenerName='" + listenerName + '\'' +
                ", messageSelector='" + messageSelector + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Registration that = (Registration) o;
        return Objects.equals(destinationName, that.destinationName) &&
                Objects.equals(listenerName, that.listenerName) &&
                Objects.equals(messageSelector, that.messageSelector);
    }

    @Override
    public int hashCode() {
        return Objects.hash(destinationName, listenerName, messageSelector);
    }
}
