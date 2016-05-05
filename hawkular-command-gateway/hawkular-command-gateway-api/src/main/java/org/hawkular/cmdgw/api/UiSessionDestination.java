/*
 * Copyright 2014-2016 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.cmdgw.api;

import org.hawkular.bus.common.BasicMessage;

/**
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public interface UiSessionDestination extends BasicMessage {
    /**
     * @return an ID chosen originally by the sending UI client that should make it possible to associate this response
     *         with the triggering request. This ID is supposed to be unique just within the sending WebSocket session.
     */
    String getSenderRequestId();
    void setSenderRequestId(String requestId);

    /**
     * @return the ID of a WebSocket Session that is the destination of this message.
     */
    String getDestinationSessionId();
    void setDestinationSessionId(String sessionId);
}
