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
 * This listener is going to test using {@link SpecificMessageDuplicate}
 * but being able to accept {@link SpecificMessage} messages.
 */
public class SpecificMessageDuplicateListener extends StoreAndLatchBasicMessageListener<SpecificMessageDuplicate> {
    public SpecificMessageDuplicateListener(CountDownLatch latch, ArrayList<SpecificMessageDuplicate> messages,
            ArrayList<String> errors) {
        super(latch, messages, errors, SpecificMessageDuplicate.class);
    }

    @Override
    protected String convertReceivedMessageClassNameToDesiredMessageClassName(String className) {
        if (className.equals(SpecificMessage.class.getName())) {
            return SpecificMessageDuplicate.class.getName();
        }
        throw new IllegalArgumentException(
                "This test listener should only be given messages of type [" + SpecificMessage.class
                        + "] but was given [" + className + "]");
    }
}
