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

/**
 * This is testing the ability to subclass a generic listener superclass and
 * have the JSON decoding work properly by simply using reflection to
 * determine the class of T (in this test case, T is SpecificMessage).
 */
public class SpecificMessageStoreAndLatchListener extends StoreAndLatchBasicMessageListener<SpecificMessage> {
    public SpecificMessageStoreAndLatchListener(CountDownLatch latch, ArrayList<SpecificMessage> messages,
            ArrayList<String> errors) {
        super(latch, messages, errors, SpecificMessage.class);
    }
}
