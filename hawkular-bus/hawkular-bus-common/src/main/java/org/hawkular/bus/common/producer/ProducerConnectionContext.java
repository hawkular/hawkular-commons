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
package org.hawkular.bus.common.producer;

import java.io.IOException;

import javax.jms.JMSException;
import javax.jms.MessageProducer;

import org.hawkular.bus.common.ConnectionContext;

public class ProducerConnectionContext extends ConnectionContext {
    private MessageProducer producer;

    public MessageProducer getMessageProducer() {
        return producer;
    }

    public void setMessageProducer(MessageProducer producer) {
        this.producer = producer;
    }

    @Override
    public void close() throws IOException {
        if (producer != null) {
            try {
                producer.close();
            } catch (JMSException e) {
                throw new IOException(e);
            }
        }

        super.close();
    }
}
