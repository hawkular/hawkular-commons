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
package org.hawkular.cmdgw.command.bus;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.jms.ConnectionFactory;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.hawkular.cmdgw.Constants;
import org.hawkular.cmdgw.log.GatewayLoggers;
import org.hawkular.cmdgw.log.MsgLogger;

/**
 * A lazy JNDI lookup of {@link ConnectionFactory} bound to the name {@link Constants#CONNECTION_FACTORY_JNDI} with
 * retries (see {@link #connectionFactoryRetryAfterMs}) and timeout {@link #connectionFactoryLookupTimeoutMs}.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@ApplicationScoped
@Default
public class BusConnectionFactoryProvider {
    private static final MsgLogger log = GatewayLoggers.getLogger(BusConnectionFactoryProvider.class);
    private volatile ConnectionFactory connectionFactory;
    private final Object connectionFactoryLock = new Object();
    private final int connectionFactoryLookupTimeoutMs =
            Integer.parseInt(System.getProperty(Constants.CONNECTION_FACTORY_JNDI_LOOKUP_TIMEOUT_MS,
                    String.valueOf(Constants.CONNECTION_FACTORY_JNDI_LOOKUP_TIMEOUT_MS_DEFAULT)));
    private final int connectionFactoryRetryAfterMs =
            Integer.parseInt(System.getProperty(Constants.CONNECTION_FACTORY_JNDI_LOOKUP_RETRY_AFTER_MS,
                    String.valueOf(Constants.CONNECTION_FACTORY_JNDI_LOOKUP_RETRY_AFTER_MS_DEFAULT)));

    /**
     * Returns an instance of {@link ConnectionFactory} looked up lazily using the JNDI name
     * {@link Constants#CONNECTION_FACTORY_JNDI}. The lookup is retried if necessary (see
     * {@link #connectionFactoryRetryAfterMs}) and a {@link RuntimeException} is thrown if the timeout set in
     * {@link #connectionFactoryLookupTimeoutMs} is exceeded.
     *
     * @return an instance of {@link ConnectionFactory}
     * @throws RuntimeException if the timeout set in {@link #connectionFactoryLookupTimeoutMs} is exceeded or if a
     *             {@link NamingException} other than {@link NameNotFoundException} occurs.
     */
    public ConnectionFactory getConnectionFactory() {
        if (connectionFactory == null) {
            synchronized (connectionFactoryLock) {
                final long start = System.currentTimeMillis();
                int attemptCount = 0;
                while (true) {
                    try {
                        attemptCount++;
                        connectionFactory =
                                (ConnectionFactory) new InitialContext().lookup(Constants.CONNECTION_FACTORY_JNDI);
                    } catch (NameNotFoundException e) {
                        /* we will retry only if the name is not found */
                        log.debugf(e, "Attempt no. [%d] to lookup [%s] was not successful. Trying again in [%d] ms.",
                                (Object) Integer.valueOf(attemptCount),
                                Constants.CONNECTION_FACTORY_JNDI,
                                (Object) Integer.valueOf(connectionFactoryRetryAfterMs));
                    } catch (NamingException e) {
                        /* No retries on other exceptions than name not found */
                        throw new RuntimeException(
                                log.errFailedToLookupConnectionFactory(ConnectionFactory.class.getName(),
                                        Constants.CONNECTION_FACTORY_JNDI),
                                e);
                    }
                    if (connectionFactory != null) {
                        log.debugf("Attempt no. [%d] to lookup [%s] succeeded.",
                                (Object) Integer.valueOf(attemptCount),
                                Constants.CONNECTION_FACTORY_JNDI);
                        break;
                    } else if (start + connectionFactoryLookupTimeoutMs < System.currentTimeMillis()) {
                        /* still time to retry */
                        log.debugf("Attempt no. [%d] to lookup [%s] was not successful. Trying again in [%d] ms.",
                                (Object) Integer.valueOf(attemptCount),
                                Constants.CONNECTION_FACTORY_JNDI,
                                (Object) Integer.valueOf(connectionFactoryRetryAfterMs));
                        try {
                            Thread.sleep(connectionFactoryRetryAfterMs);
                        } catch (InterruptedException e1) {
                            throw new RuntimeException(e1);
                        }
                    } else {
                        /* timeout */
                        throw new RuntimeException(
                                log.errFailedToLookupConnectionFactory(ConnectionFactory.class.getName(),
                                        Constants.CONNECTION_FACTORY_JNDI, connectionFactoryLookupTimeoutMs));
                    }
                }
            }
        }
        return connectionFactory;
    }
}
