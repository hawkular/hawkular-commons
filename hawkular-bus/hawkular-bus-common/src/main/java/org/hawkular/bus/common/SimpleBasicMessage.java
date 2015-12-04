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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
/**
 * A simple message that is sent over the message bus.
 */
public class SimpleBasicMessage extends AbstractMessage {
    // the basic message body - it will be exposed to the JSON output
    @JsonInclude
    private String message;

    // some optional additional details about the basic message
    @JsonInclude
    private Map<String, String> details;

    protected SimpleBasicMessage() {
        // Intentionally left blank
    }

    public SimpleBasicMessage(String message) {
        this(message, null);
    }

    public SimpleBasicMessage(String message, Map<String, String> details) {
        this.message = message;

        // make our own copy of the details data
        if (details != null && !details.isEmpty()) {
            this.details = new HashMap<String, String>(details);
        } else {
            this.details = null;
        }
    }

    /**
     * The simple message string of this message.
     *
     * @return message string
     */
    public String getMessage() {
        return message;
    }

    /**
     * Allow subclasses to set the message
     */
    protected void setMessage(String msg) {
        this.message = msg;
    }

    /**
     * Optional additional details about this message. This could be null if there are no additional details associated
     * with this message.
     *
     * @return the details of this message or null. This is an unmodifiable, read-only map of details.
     */
    public Map<String, String> getDetails() {
        if (details == null) {
            return null;
        }
        return Collections.unmodifiableMap(details);
    }
}
