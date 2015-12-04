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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.hawkular.bus.common.AbstractMessage;
import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.hawkular.bus.common.consumer.BasicMessageListener;

/**
 * Simple test listener that allows you to wait for a message and when it comes in you can retrieve it. See
 * {@link #waitForMessage(long)} and {@link #getReceivedMessage()}. This can retrieve multiple messages serially,
 * but if you don't retrieve a message before a new one comes in, the first message is lost.
 *
 * This class is not thread safe. Its purpose is just to fascilitate unit tests.
 *
 * @param <T> the expected message type
 */
public class SimpleTestListener<T extends AbstractMessage> extends BasicMessageListener<T> {
    private CountDownLatch latch = new CountDownLatch(1);
    public T message;

    public SimpleTestListener(Class<T> clazz) {
        super(clazz);
    }

    public boolean waitForMessage(long secs) throws InterruptedException {
        return latch.await(secs, TimeUnit.SECONDS);
    }

    public T getReceivedMessage() {
        T result = null;
        if (message != null) {
            result = message;
            // reset the listener to get ready for the next message
            latch = new CountDownLatch(1);
            message = null;
        }
        return result;
    }

    @Override
    protected void onBasicMessage(BasicMessageWithExtraData<T> message) {
        this.message = message.getBasicMessage();
        latch.countDown();
    }
}
