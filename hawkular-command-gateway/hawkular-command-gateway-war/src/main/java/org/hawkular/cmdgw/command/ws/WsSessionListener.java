/*
 * Copyright 2014-2017 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.cmdgw.command.ws;

import javax.websocket.Session;

/**
 * A simple listener that is attached to a single websocket {@link Session}.
 * <p>
 * When a new websocket client connects, this object's {@link #sessionAdded()} method will be called
 * informing this object that a new client is ready to begin sending commands over the websocket.
 * <p>
 * When a old websocket client disconnects, this object's {@link #sessionRemoved()} method will be called
 * informing this object that it should clean up any resources or other connections that are no longer
 * needed now that the client has gone away.
 * <p>
 * Note on concurrency: The implementors are warranted that {@link #sessionAdded()} will be called before
 * {@link #sessionRemoved()} and that there will be no overlapping calls of these methods from distinct threads. Further
 * best effort will be done to call {@link #sessionRemoved()} for all existing listeners on application exit.
 *
 * @see WsSessions#addWsSessionListenerProducer(java.util.function.BiFunction)
 * @see WsSessions#removeWsSessionListenerProducer(java.util.function.BiFunction)
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public interface WsSessionListener {
    /**
     * Notifies this listener that the {@link Session} to which this listener is attached was successfully added to
     * {@link WsSessions}.
     */
    void sessionAdded();

    /**
     * Notifies this listener that the {@link Session} to which this listener is attached was removed from
     * {@link WsSessions}.
     */
    void sessionRemoved();
}
