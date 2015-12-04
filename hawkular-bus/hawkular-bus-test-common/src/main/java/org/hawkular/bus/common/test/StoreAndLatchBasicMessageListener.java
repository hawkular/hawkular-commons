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

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

import javax.jms.JMSException;

import org.hawkular.bus.common.AbstractMessage;
import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.hawkular.bus.common.consumer.BasicMessageListener;

/**
 * Creates a simple test listener. This listener will log messages it receives
 * and errors it encounters in lists given to it via its constructor.
 *
 * This listener will notify when it gets a message by counting down a latch.
 */
public class StoreAndLatchBasicMessageListener<T extends AbstractMessage> extends BasicMessageListener<T> {

    private final CountDownLatch latch;
    private final ArrayList<T> messages;
    private final ArrayList<String> errors;

    public StoreAndLatchBasicMessageListener(CountDownLatch latch, ArrayList<T> messages, ArrayList<String> errors,
            Class<T> basicMessageClass) {
        super(basicMessageClass);
        this.latch = latch;
        this.messages = messages;
        this.errors = errors;
    }

    @Override
    public void onBasicMessage(BasicMessageWithExtraData<T> msgWithData) {
        try {
            storeMessage(msgWithData.getBasicMessage());
        } catch (Exception ex) {
            storeError(ex);
        } finally {
            countDownLatch();
        }
    }

    protected CountDownLatch getLatch() {
        return latch;
    }

    protected ArrayList<T> getMessages() {
        return messages;
    }

    protected ArrayList<String> getErrors() {
        return errors;
    }

    protected void countDownLatch() {
        CountDownLatch l = getLatch();
        if (l != null) {
            l.countDown();
        }
    }

    protected void storeError(Exception exception) {
        ArrayList<String> e = getErrors();
        if (e != null) {
            e.add(exception.toString());
        } else {
            exception.printStackTrace();
        }
    }

    protected void storeMessage(T message) throws JMSException {
        ArrayList<T> m = getMessages();
        if (m != null) {
            m.add(message);
        }
    }
}
