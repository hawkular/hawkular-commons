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

import org.hawkular.bus.broker.EmbeddedBroker;

/**
 * Wrapper around an embedded broker. Subclasses provide concrete
 * implementations using different connectors (e.g. a in-memory broker or a
 * broker listening on a TCP port).
 */
public abstract class AbstractEmbeddedBrokerWrapper {
    private EmbeddedBroker broker;

    public void setBroker(EmbeddedBroker b) {
        if (b == null) {
            throw new NullPointerException("broker must not be null");
        }
        broker = b;
    }

    public EmbeddedBroker getBroker() {
        return broker;
    }

    public void start() throws Exception {
        getBroker().startBroker();
    }

    public void stop() throws Exception {
        getBroker().stopBroker();
    }

    public abstract String getBrokerURL();
}
