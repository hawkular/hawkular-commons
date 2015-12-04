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
package org.hawkular.bus.common.log;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

/**
 * @author John Mazzitelli
 */
@MessageLogger(projectCode = "HAWKBUS")
@ValidIdRange(min = 100000, max = 109999)
public interface MsgLogger extends BasicLogger {
    MsgLogger LOGGER = Logger.getMessageLogger(MsgLogger.class, MsgLogger.class.getPackage().getName());

    @LogMessage(level = Level.ERROR)
    @Message(id = 100000, value = "A message was received that was not a valid text message")
    void errorNotValidTextMessage(@Cause Throwable cause);

    @LogMessage(level = Level.ERROR)
    @Message(id = 100001, value = "A message was received that was not a valid JSON-encoded AbstractMessage object")
    void errorNotValidJsonMessage(@Cause Throwable cause);

    @LogMessage(level = Level.ERROR)
    @Message(id = 100002, value = "Cannot close the previous connection; memory might leak.")
    void errorCannotCloseConnectionMemoryMightLeak(@Cause Throwable t);

    @LogMessage(level = Level.ERROR)
    @Message(id = 100003, value = "Failed to start connection.")
    void errorFailedToStartConnection(@Cause Throwable t);

    @LogMessage(level = Level.ERROR)
    @Message(id = 100004, value = "Cannot return response - there is no message sender assigned to this listener")
    void errorNoMessageSenderInListener();

    @LogMessage(level = Level.ERROR)
    @Message(id = 100005, value = "Cannot return response - there is no connection context assigned to this listener")
    void errorNoConnectionContextInListener();

    @LogMessage(level = Level.ERROR)
    @Message(id = 100006, value = "Cannot return response - no session in the conn context assigned to this listener")
    void errorNoSessionInListener();

    @LogMessage(level = Level.ERROR)
    @Message(id = 100007, value = "Failed to send response")
    void errorFailedToSendResponse(@Cause Throwable t);

    @LogMessage(level = Level.ERROR)
    @Message(id = 100008, value = "Told not to interrupt if running, but it is running. Cannot cancel.")
    void errorCannotCancelRunningFuture();

    @LogMessage(level = Level.ERROR)
    @Message(id = 100009, value = "Failed to close consumer, cannot fully cancel")
    void errorConsumerCloseFailureOnFutureCancel();

    @LogMessage(level = Level.ERROR)
    @Message(id = 100010, value = "Failed to store incoming message for some reason. This future is now invalid.")
    void errorCannotStoreIncomingMessageFutureInvalid();

    @LogMessage(level = Level.ERROR)
    @Message(id = 100011, value = "Failed to close consumer in future")
    void errorFailedToCloseFutureConsumer(@Cause Throwable t);

    @LogMessage(level = Level.ERROR)
    @Message(id = 100012, value = "Failed to close resources used to reply to RPC client")
    void errorFailedToCloseResourcesToRPCClient(@Cause Throwable t);
}
