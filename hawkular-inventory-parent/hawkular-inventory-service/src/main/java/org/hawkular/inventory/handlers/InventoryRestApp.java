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
package org.hawkular.inventory.handlers;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.hawkular.inventory.log.InventoryLoggers;
import org.hawkular.inventory.log.MsgLogger;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@ApplicationPath("/")
public class InventoryRestApp extends Application {
    private static final MsgLogger log = InventoryLoggers.getLogger(InventoryRestApp.class);

    public static final String TENANT_HEADER_NAME = "Hawkular-Tenant";

    public InventoryRestApp() {
        log.infoInventoryAppStarted();
    }

}
