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
package org.hawkular.inventory.log;

import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

@MessageLogger(projectCode = "HAWKINV")
@ValidIdRange(min = 100000, max = 199999)
public interface MsgLogger {
    MsgLogger LOGGER = Logger.getMessageLogger(MsgLogger.class, MsgLogger.class.getPackage().getName());

    @LogMessage(level = Level.INFO)
    @Message(id = 100000, value = "Inventory App Started")
    void infoInventoryAppStarted();

    @LogMessage(level = Level.ERROR)
    @Message(id = 100001, value = "Inventory backend cache is not found")
    void errorInventoryCacheNotFound();

    @LogMessage(level = Level.ERROR)
    @Message(id = 100002, value = "Inventory configuration cache is not found")
    void errorInventoryCacheConfigurationNotFound(@Cause Throwable e);

    @LogMessage(level = Level.INFO)
    @Message(id = 100003, value = "Inventory is reindexing caches")
    void infoStartInventoryReindex();

    @LogMessage(level = Level.INFO)
    @Message(id = 100004, value = "Inventory finished reindexing in [%s] ms")
    void infoStopInventoryReindex(long time);

    @LogMessage(level = Level.ERROR)
    @Message(id = 100005, value = "Error reindexing caches")
    void errorReindexingCaches(@Cause Throwable e);

    @LogMessage(level = Level.ERROR)
    @Message(id = 100006, value = "Error reading inventory disk")
    void errorReadingInventoryDisk(@Cause Throwable e);

    @LogMessage(level = Level.ERROR)
    @Message(id = 100007, value = "Error registering MBean")
    void errorRegisteringMBean(@Cause Throwable e);

    @LogMessage(level = Level.ERROR)
    @Message(id = 100008, value = "Error unregistering MBean")
    void errorUnregisteringMBean(@Cause Throwable e);

    @LogMessage(level = Level.INFO)
    @Message(id = 100009, value = "Changing polling stats interval to [%s] ms")
    void infoChangingPollingStatsInterval(long newPollingInterval);

}
