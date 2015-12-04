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

/**
 * Just a simple container that contains a AbstractMessage and some (optional) binary data associated with the message.
 */
public class BasicMessageWithExtraData<T extends BasicMessage> {
    private final T basicMessage;
    private final BinaryData binaryData;

    public BasicMessageWithExtraData(T basicMessage, BinaryData binaryData) {
        if (basicMessage == null) {
            throw new IllegalArgumentException("basicMessage cannot be null");
        }

        this.basicMessage = basicMessage;
        this.binaryData = binaryData;

    }

    public T getBasicMessage() {
        return basicMessage;
    }

    /**
     * @return if the message is associated with extra binary data, this is that data. May be <code>null</code>.
     */
    public BinaryData getBinaryData() {
        return binaryData;
    }
}
