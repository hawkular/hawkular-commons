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

import java.util.Map;
import java.util.Objects;

/**
 * @author jsanda
 */
public class BasicTextMessage extends AbstractMessage {

    private String text;

    private BasicTextMessage() {
    }

    public BasicTextMessage(String text) {
        this.text = text;
    }

    public BasicTextMessage(MessageId id, String text) {
        this.text = text;
        setMessageId(id);
    }

    public BasicTextMessage(String text, Map<String, String> headers) {
        this.text = text;
        setHeaders(headers);
    }

    public BasicTextMessage(MessageId id, String text, Map<String, String> headers) {
        this.text = text;
        setMessageId(id);
        setHeaders(headers);
    }

    public String getText() {
        return text;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BasicTextMessage that = (BasicTextMessage) o;
        return Objects.equals(text, that.text);
    }

    @Override public int hashCode() {
        return Objects.hash(text);
    }
}
