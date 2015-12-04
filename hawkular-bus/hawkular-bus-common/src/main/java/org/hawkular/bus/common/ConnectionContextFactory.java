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

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.hawkular.bus.common.consumer.ConsumerConnectionContext;
import org.hawkular.bus.common.log.MsgLogger;
import org.hawkular.bus.common.producer.ProducerConnectionContext;
import org.jboss.logging.Logger;

/**
 * Provides convenience functionality to create {@link ProducerConnectionContext producer} or
 * {@link ConsumerConnectionContext consumer} contexts. You can then pass these created contexts to
 * {@link MessageProcessor} to send and receive messages.
 *
 * This class can reuse a connection so that it is shared across multiple contexts. See
 * {@link #createOrReuseConnection(ConnectionContext, boolean)}. If this object was told not to reuse
 * its connection, it will create a new connection for each context is creates.
 *
 * When you are done with sending and receiving messages through the created contexts, you should call {@link #close()}
 * to free up resources and close the connection to the broker.
 *
 * Subclasses are free to extend this class to add or override functionality or to provide stricter type-checking.
 */
public class ConnectionContextFactory implements AutoCloseable {

    private final MsgLogger msglog = MsgLogger.LOGGER;
    private final Logger log = Logger.getLogger(ConnectionContextFactory.class);
    private boolean reuseConnection;

    protected ConnectionFactory connectionFactory;

    private Connection connection;

    /**
     * Initializes with the given factory.
     *
     * @param connectionFactory the factory that will be used to create contexts.
     *
     * @throws JMSException any error
     */
    public ConnectionContextFactory(ConnectionFactory connectionFactory) throws JMSException {
        this(false, connectionFactory);
    }

    /**
     * Initializes with the given factory.
     *
     * @param reuseConnection if true this factory will reuse its connection so contexts it creates can share it.
     *                        Use this with caution because if a shared connection is closed, all contexts created
     *                        with that connection will no longer work.
     * @param connectionFactory the factory that will be used to create contexts.
     *
     * @throws JMSException any error
     */
    public ConnectionContextFactory(boolean reuseConnection, ConnectionFactory connectionFactory) throws JMSException {
        this.reuseConnection = reuseConnection;
        this.connectionFactory = connectionFactory;
        log.debugf("%s has been created with an existing connection factory: %s", this.getClass().getSimpleName(),
                connectionFactory);
    }

    /**
     * Creates a new producer connection context, reusing any existing connection that might have already been created.
     * The destination of the connection's session will be that of the given endpoint.
     *
     * @param endpoint where the producer will send messages
     * @return the new producer connection context fully populated
     * @throws JMSException any error
     */
    public ProducerConnectionContext createProducerConnectionContext(Endpoint endpoint) throws JMSException {
        ProducerConnectionContext context = new ProducerConnectionContext();
        createOrReuseConnection(context, true);
        createSession(context);
        createDestination(context, endpoint);
        createProducer(context);
        return context;
    }

    /**
     * Creates a new consumer connection context, reusing any existing connection that might have already been created.
     * The destination of the connection's session will be that of the given endpoint.
     *
     * @param endpoint where the consumer will listen for messages
     * @return the new consumer connection context fully populated
     * @throws JMSException any error
     */
    public ConsumerConnectionContext createConsumerConnectionContext(Endpoint endpoint) throws JMSException {
        return createConsumerConnectionContext(endpoint, null);
    }

    /**
     * Creates a new consumer connection context, reusing any existing connection that might have
     * already been created. The destination of the connection's session will be that of the given endpoint.
     * The consumer will filter messages based on the given message selector expression (which may be
     * null in which case the consumer will consume all messages).
     *
     * @param endpoint where the consumer will listen for messages
     * @param messageSelector message consumer's message selector expression.
     * @return the new consumer connection context fully populated
     * @throws JMSException any error
     */
    public ConsumerConnectionContext createConsumerConnectionContext(Endpoint endpoint, String messageSelector)
            throws JMSException {
        ConsumerConnectionContext context = new ConsumerConnectionContext();
        createOrReuseConnection(context, true);
        createSession(context);
        createDestination(context, endpoint);
        createConsumer(context, messageSelector);
        return context;
    }

    /**
     * This will close its open connection that it has cached, thus freeing up resources.
     * This method should be called when this context factory is no longer needed. But realize
     * that any contexts that were created with the cached connection will be invalidated
     * since this method will close that connection.
     *
     * @throws JMSException any error
     */
    @Override
    public void close() throws JMSException {
        cacheConnection(null, true);
        log.debugf("%s has been closed", this);
    }

    /**
     * @return true if this factory will reuse its connection. Otherwise,
     *         it will always create new connections for each context it creates.
     */
    protected boolean isReuseConnection() {
        return reuseConnection;
    }

    protected ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * The stored connection.
     *
     * NOTE: This is not necessarily the connection created via calling {@link #createConnection(ConnectionContext)}.
     *
     * @return the connection
     *
     * @see #createConnection(ConnectionContext)
     */
    protected Connection getConnection() {
        return connection;
    }

    /**
     * To store a connection in this processor object, call this setter.
     * If there was already a cached connection, it will be closed.
     *
     * NOTE: Calling {@link #createConnection(ConnectionContext)} does
     * <b>not</b> set this processor's connection - that method only creates the
     * connection and puts that connection in the context. It does not save that
     * connection in this processor object. You must explicitly set the
     * connection via this method if you want that connection cached here. See
     * also {@link #createOrReuseConnection(ConnectionContext, boolean)}.
     *
     * @param connection the connection
     * @param closeExistingConnection if true, and if there was already a connection
     *                                cached, that connection will be closed. Otherwise
     *                                it will be left alone but the new connection
     *                                will take its place.
     *
     * @see #createOrReuseConnection(ConnectionContext, boolean)
     */
    protected void cacheConnection(Connection connection, boolean closeExistingConnection) {
        if (this.connection != null && closeExistingConnection) {
            try {
                // make sure it is closed to free up any resources it was using
                this.connection.close();
            } catch (JMSException e) {
                msglog.errorCannotCloseConnectionMemoryMightLeak(e);
            }
        }

        this.connection = connection;
    }

    /**
     * This method provides a way to cache and share a connection across
     * multiple contexts. It combines the creation and setting of the
     * connection. This also can optionally start the connection immediately.
     * Use this if you want to reuse any connection that may already be stored
     * in this processor object (i.e. {@link #getConnection()} is non-null). If
     * there is no connection yet, one will be created. Whether the connection
     * is created or reused, that connection will be stored in the given
     * context.
     *
     * Note that if this object was told not to cache connections, this method
     * will always create a new connection and store it in this object, overwriting
     * any previously created connection (see {@link #cacheConnection}).
     *
     * @param context the connection will be stored in this context
     * @param start if true, the created connection will be started.
     * @throws JMSException any error
     */
    protected void createOrReuseConnection(ConnectionContext context, boolean start) throws JMSException {
        Connection conn;

        if (isReuseConnection()) {
            conn = getConnection();
            if (conn != null) {
                // already have a connection cached, give it to the context
                context.setConnection(conn);
            } else {
                // there is no connection yet; create it and cache it
                createConnection(context);
                conn = context.getConnection();
                cacheConnection(conn, false);
            }
        } else {
            // we are not to cache connections - always create one
            createConnection(context);
            conn = context.getConnection();
            cacheConnection(conn, false);
        }

        if (start) {
            // Calling start on started connection is ignored.
            // But if an exception is thrown, we need to throw away the connection
            try {
                conn.start();
            } catch (JMSException e) {
                msglog.errorFailedToStartConnection(e);
                cacheConnection(null, true);
                throw e;
            }
        }
    }

    /**
     * Creates a connection using this object's connection factory and stores
     * that connection in the given context object.
     *
     * NOTE: this does <b>not</b> set the connection in this processor object.
     * If the caller wants the created connection cached in this processor
     * object, {@link #cacheConnection} must be passed the connection
     * found in the context after this method returns. See also
     * {@link #createOrReuseConnection(ConnectionContext, boolean)}.
     *
     * @param context the context where the new connection is stored
     * @throws JMSException any error
     * @throws IllegalStateException if the context is null
     *
     * @see #createOrReuseConnection(ConnectionContext, boolean)
     * @see #cacheConnection
     */
    protected void createConnection(ConnectionContext context) throws JMSException {
        if (context == null) {
            throw new IllegalStateException("The context is null");
        }
        ConnectionFactory factory = getConnectionFactory();
        Connection conn = factory.createConnection();
        context.setConnection(conn);
    }

    /**
     * Creates a default session using the context's connection. This implementation creates a non-transacted,
     * auto-acknowledged session. Subclasses are free to override this behavior.
     *
     * @param context the context where the new session is stored
     * @throws JMSException any error
     * @throws IllegalStateException if the context is null or the context's connection is null
     */
    protected void createSession(ConnectionContext context) throws JMSException {
        if (context == null) {
            throw new IllegalStateException("The context is null");
        }
        Connection conn = context.getConnection();
        if (conn == null) {
            throw new IllegalStateException("The context had a null connection");
        }
        Session session = conn.createSession(false, Session.AUTO_ACKNOWLEDGE);
        context.setSession(session);
    }

    /**
     * Creates a destination using the context's session. The destination correlates to the given named queue or topic.
     *
     * @param context the context where the new destination is stored
     * @param endpoint identifies the queue or topic
     * @throws JMSException any error
     * @throws IllegalStateException if the context is null or the context's session is null or endpoint is null
     */
    protected void createDestination(ConnectionContext context, Endpoint endpoint) throws JMSException {
        if (endpoint == null) {
            throw new IllegalStateException("Endpoint is null");
        }
        if (context == null) {
            throw new IllegalStateException("The context is null");
        }
        Session session = context.getSession();
        if (session == null) {
            throw new IllegalStateException("The context had a null session");
        }
        Destination dest;
        if (endpoint.getType() == Endpoint.Type.QUEUE) {
            if (endpoint.isTemporary()) {
                dest = session.createTemporaryQueue();
            } else {
                dest = session.createQueue(getDestinationName(endpoint));
            }
        } else {
            if (endpoint.isTemporary()) {
                dest = session.createTemporaryTopic();
            } else {
                dest = session.createTopic(getDestinationName(endpoint));
            }
        }
        context.setDestination(dest);
    }

    /**
     * With ActiveMQ we were passing the full JNDI name, e.g., java:/topic/HawkularInventoryChanges, to
     * Session.createTopic (or Session.createQueue). Using the full JNDI name/path does not work with Artemis.
     */
    private String getDestinationName(Endpoint endpoint) {
        int start = endpoint.getName().lastIndexOf('/') + 1;
        return endpoint.getName().substring(start);
    }

    /**
     * Creates a message producer using the context's session and destination.
     *
     * @param context the context where the new producer is stored
     * @throws JMSException any error
     * @throws IllegalStateException if the context is null or the context's session is null
     *                               or the context's destination is null
     */
    protected void createProducer(ProducerConnectionContext context) throws JMSException {
        if (context == null) {
            throw new IllegalStateException("The context is null");
        }
        Session session = context.getSession();
        if (session == null) {
            throw new IllegalStateException("The context had a null session");
        }
        Destination dest = context.getDestination();
        if (dest == null) {
            throw new IllegalStateException("The context had a null destination");
        }
        MessageProducer producer = session.createProducer(dest);
        context.setMessageProducer(producer);
    }

    /**
     * Creates a message consumer using the context's session and destination.
     *
     * @param context the context where the new consumer is stored
     * @param messageSelector the message selector expression that the consumer will use to filter messages
     * @throws JMSException any error
     * @throws IllegalStateException if the context is null or the context's session is null
     *                               or the context's destination is null
     */
    protected void createConsumer(ConsumerConnectionContext context, String messageSelector) throws JMSException {
        if (context == null) {
            throw new IllegalStateException("The context is null");
        }
        Session session = context.getSession();
        if (session == null) {
            throw new IllegalStateException("The context had a null session");
        }
        Destination dest = context.getDestination();
        if (dest == null) {
            throw new IllegalStateException("The context had a null destination");
        }
        MessageConsumer consumer = session.createConsumer(dest, messageSelector);
        context.setMessageConsumer(consumer);
    }
}
