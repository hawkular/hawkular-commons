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
package org.hawkular.cmdgw.command.ws;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiFunction;

import javax.websocket.CloseReason;
import javax.websocket.Session;

import org.hawkular.cmdgw.log.GatewayLoggers;
import org.hawkular.cmdgw.log.MsgLogger;

/**
 * Maintains a map of websocket {@link Session}s currently connected with the server.
 * The choice of the key for the map is up to the caller.
 * <p>
 * A {@code sessionId} of a UI client's {@link Session} or a feed's {@code feedId} are the most prominent
 * candidates for keys.
 * <p>
 * A thread safety note: this class uses a {@link ReadWriteLock} internally, hence its public methods can be safely
 * called from multiple threads.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
public class WsSessions {

    /**
     * A websocket {@link Session} with its attached {@link WsSessionListener}s.
     */
    private static class SessionEntry {
        private final Session session;
        private final List<WsSessionListener> sessionListeners;

        private SessionEntry(Session session, List<WsSessionListener> sessionListeners) {
            super();
            this.session = session;
            this.sessionListeners = sessionListeners;
        }

        /**
         * Call {@link WsSessionListener#sessionAdded()} on the attached {@link WsSessionListener}s.
         */
        public void added() {
            for (WsSessionListener listener : sessionListeners) {
                listener.sessionAdded();
            }
        }

        public Session getSession() {
            return session;
        }

        /**
         * Call {@link WsSessionListener#sessionRemoved()} on the attached {@link WsSessionListener}s.
         */
        public void removed() {
            for (WsSessionListener listener : sessionListeners) {
                listener.sessionRemoved();
            }
        }
    }

    private static final MsgLogger log = GatewayLoggers.getLogger(WsSessions.class);

    private final String endpoint;
    private final ReadWriteLock sessionsLock = new ReentrantReadWriteLock(true);
    private final Lock sessionsLockRead = sessionsLock.readLock();
    private final Lock sessionsLockWrite = sessionsLock.writeLock();
    /** key is feedId or sessionId, value is the {@link Session} with its attached {@link WsSessionListener}s */
    private final Map<String, SessionEntry> sessions = new HashMap<>();
    private final List<BiFunction<String, Session, WsSessionListener>> wsSessionListenerProducers //
    = new CopyOnWriteArrayList<>();

    public WsSessions(String endpoint) {
        super();
        this.endpoint = endpoint;
    }

    /**
     * Stores the given {@code newSession} under the given {@code key}.
     * <p>
     * If there is already a session associated with the given {@code key}, the given {@code newSession} is closed;
     * it is not added or associated with the given key and an error will be logged. The original session remains
     * associated with the {@code key}.
     * <p>
     * When adding a session, that new session's websocket session listeners will be told via
     * {@link WsSessionListener#sessionAdded()}.
     *
     * @param key the key (feedId or sessionId) that will be associated with the new session
     * @param newSession the new session to add
     * @return {@code true} if the session was added; {@code false} otherwise.
     */
    public boolean addSession(String key, Session newSession) {

        SessionEntry newEntry = createSessionEntry(key, newSession);
        SessionEntry oldEntry = null;

        sessionsLockWrite.lock();
        try {
            oldEntry = this.sessions.putIfAbsent(key, newEntry);
        } finally {
            sessionsLockWrite.unlock();
        }

        /* check how successful we were with adding */
        if (oldEntry == null) {
            log.debugf("A WebSocket session [%s] of [%s] has been added. The endpoint has now [%d] sessions", key,
                    endpoint, this.sessions.size());
            newEntry.added();
        } else {
            /* a feed already had a session open, cannot open more than one */
            try {
                log.errorClosingDuplicateWsSession(key, endpoint);
                newSession.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY,
                        "Cannot have multiple WebSocket sessions open, the new one will be closed"));
            } catch (Exception e) {
                log.errorCannotCloseDuplicateWsSession(key, endpoint, e);
            }
        }

        return oldEntry == null;
    }

    /**
     * Add the given {@code wsSessionListenerProducers} to the internal list of {@link WsSessionListener} producers.
     *
     * A listener-producer is just a bi-function that takes a key string (like a UI client session ID or a feed ID)
     * and a websocket {@link Session} and produces a websocket session listener. A websocket session listener
     * has the job of performing tasks when a websocket client connects and disconnects (for example, when a
     * websocket client connects and its websocket session is created, a websocket session listener will then need to
     * add bus listeners so the websocket client's messages can be processed properly).
     *
     * @param wsSessionListenerProducer a function that produces {@link WsSessionListener} for a given pair of
     *        {@code sessionKey} and {@link Session}.
     */
    public void addWsSessionListenerProducer(BiFunction<String, Session, WsSessionListener> wsSessionListenerProducer) {
        wsSessionListenerProducers.add(wsSessionListenerProducer);
    }

    /**
     * Creates a new {@link SessionEntry} for the given {@code sessionKey} and {@code session}. Iterates over
     * {@link #wsSessionListenerProducers} to get {@link WsSessionListener}s that will be attached to the given
     * {@code session}.
     * <p>
     * Note that this only produces/assigns websocket session listeners to the sessions, but the listeners are not
     * told to do anything yet.
     *
     * @param sessionKey the sessionId or feedId
     * @param session the {@link Session} for which we are creating the {@link SessionEntry}
     * @return a new {@link SessionEntry}
     */
    private SessionEntry createSessionEntry(String sessionKey, Session session) {
        List<WsSessionListener> sessionListeners = new ArrayList<>();
        for (BiFunction<String, Session, WsSessionListener> producer : wsSessionListenerProducers) {
            WsSessionListener sessionListener = producer.apply(sessionKey, session);
            if (sessionListener != null) {
                sessionListeners.add(sessionListener);
            }
        }
        return new SessionEntry(session, sessionListeners);
    }

    /**
     * Returns the {@link Session} stored under the given {@code key} or {@code null} if there is no such session.
     *
     * @param key the key to look for
     * @return the {@link Session} stored under the given {@code key} or {@code null} if there is no such session.
     */
    public Session getSession(String key) {
        sessionsLockRead.lock();
        try {
            SessionEntry entry = this.sessions.get(key);
            return entry != null ? entry.getSession() : null;
        } finally {
            sessionsLockRead.unlock();
        }
    }

    /**
     * Removes the session associated with the given {@code key}. If {@code doomedSession} is not {@code null}, the
     * session matching the given {@code key} in {@link #sessions} will only be removed if that session has the same ID
     * as the given {@code doomedSession}.
     * <p>
     * When removing a known session, that doomed session's websocket session listeners will be told via
     * {@link WsSessionListener#sessionRemoved()}.
     *
     * @param key identifies the session to be removed
     * @param doomedSession if not null, ensures that only this session will be removed
     * @return the removed session or null if nothing was removed
     */
    public void removeSession(String key, Session doomedSession) {
        SessionEntry removedEntry = null;

        // If no session was passed in, remove any session associated with the given key.
        // If a session was passed in, only remove it if the key is associated with that session.
        // This is to support the need to close extra sessions a feed might have created.

        sessionsLockWrite.lock();
        try {
            if (doomedSession == null) {
                removedEntry = this.sessions.remove(key);
            } else {
                /* here between sessions.get() and sessions.remove() the write lock is especially important */
                SessionEntry existingEntry = this.sessions.get(key);
                if (existingEntry != null && existingEntry.getSession().getId().equals(doomedSession.getId())) {
                    removedEntry = this.sessions.remove(key);
                }
            }

            if (removedEntry != null) {
                /* we really removed a session, let's call its listeners */
                removedEntry.removed();
                log.debugf(
                        "WebSocket Session [%s] of [%s] with key [%s] has been removed."
                                + " The endpoint has now [%d] sessions",
                        removedEntry.getSession().getId(), endpoint, key, this.sessions.size());
            }
        } finally {
            sessionsLockWrite.unlock();
        }

    }

    public void destroy() {
        sessionsLockWrite.lock();
        try {
            log.debugf("Destroying [%s] of [%s]. About to call remove listeners on [%d] sessions.",
                    getClass().getName(), endpoint, this.sessions.size());
            for (SessionEntry entry : sessions.values()) {
                entry.removed();
            }
            sessions.clear();
        } catch (Throwable t) {
            log.couldNotDestroy(getClass().getName(), endpoint, t);
        } finally {
            sessionsLockWrite.unlock();
        }
    }

    /**
     * Remove the given {@code wsSessionListenerProducers} from the internal list of {@link WsSessionListener}
     * producers.
     *
     * @param wsSessionListenerProducer a function that produces {@link WsSessionListener} for a given pair of
     *        {@code sessionKey} and {@link Session}.
     */
    public void removeWsSessionListenerProducer(
            BiFunction<String, Session, WsSessionListener> wsSessionListenerProducer) {
        wsSessionListenerProducers.remove(wsSessionListenerProducer);
    }
}
