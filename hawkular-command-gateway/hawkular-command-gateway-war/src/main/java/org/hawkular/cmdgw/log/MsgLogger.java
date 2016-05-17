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
package org.hawkular.cmdgw.log;

import javax.websocket.CloseReason;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

@MessageLogger(projectCode = "HAWKFEEDCOMM")
@ValidIdRange(min = 1, max = 5000)
public interface MsgLogger extends BasicLogger {

    MsgLogger LOG = Logger.getMessageLogger(MsgLogger.class, "org.hawkular.cmdgw.command.ws");

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 3, value = "Attempted to add a WebSocket session key [%s] twice for endpoint [%s]."
            + " This is a violation; closing the extra session")
    void errorClosingDuplicateWsSession(String key, String endpoint);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 4, value = "Cannot close the duplicate WebSocket session with key [%s] of endpoint [%s]")
    void errorCannotCloseDuplicateWsSession(String key, String endpoint, @Cause Throwable t);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 5, value = "UI client [%s] (session [%s]) provided an invalid command request: [%s]")
    void errorInvalidCommandRequestUIClient(String uiClientId, String sessionId, String invalidCommandRequest);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 6, value = "Failed to process message [%s] from WebSocket session [%s] of [%s]")
    void errorWsCommandExecutionFailure(String messageClass, String sessionId, String endpoint, @Cause Throwable t);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 7, value = "Cannot process an execute-operation message")
    void errorCannotProcessExecuteOperationMessage(@Cause Throwable t);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 8, value = "Received the following error message and stack trace from remote endpoint: %s\n%s")
    void warnReceivedGenericErrorResponse(String errorMessage, String stackTrack);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 10, value = "Failed to add message listeners for feed [%s]. Closing session [%s]")
    void errorFailedToAddMessageListenersForFeed(String feedId, String id, @Cause Throwable t);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 12, value = "Feed [%s] session closed. Reason=[%s]")
    void infoFeedSessionClosed(String feedId, CloseReason reason);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 13, value = "WebSocket session [%s] opened for endpoint [%s]")
    void infoWsSessionOpened(String sessionId, String endpoint);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 14, value = "Received message [%s] from WebSocket session [%s] of [%s]")
    void infoReceivedWsMessage(String uiClientId, String sessionId, String endpoint);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 15, value = "WebSocket session [%s] of [%s] closed. Reason=[%s]")
    void infoWsSessionClosed(String sessionId, String endpoint, CloseReason reason);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 16, value = "Adding listeners for feed [%s]")
    void infoAddingListenersForFeed(String feedId);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 17, value = "Failed to close consumer context; will keep trying to close the rest")
    void errorFailedClosingConsumerContext(@Cause Throwable t);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 18, value = "Removing listeners for feed [%s]")
    void infoRemovingListenersForFeed(String feedId);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 19, value = "Failed to removing listeners for feed [%s]")
    void errorFailedRemovingListenersForFeed(String feedId, @Cause Throwable t);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 20, value = "Received binary data from feed [%s]")
    void infoReceivedBinaryDataFromFeed(String feedId);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 21, value = "Received message [%s] with binary data from WebSocket session [%s] of [%s]")
    void infoReceivedBinaryData(String messageClass, String sessionId, String endpoint);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 22, value = "Adding listeners for UI client [%s]")
    void infoAddingListenersForUIClient(String uiClientId);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 23, value = "Removing listeners for UI client [%s]")
    void infoRemovingListenersForUIClient(String uiClientId);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 24, value = "Failed to removing listeners for UI client [%s]")
    void errorFailedRemovingListenersForUIClient(String uiClientId, @Cause Throwable t);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 25, value = "Failed to add message listeners for UI client [%s]. Closing session [%s]")
    void errorFailedToAddMessageListenersForUIClient(String uiClientId, String sessionId, @Cause Throwable t);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 26, value = "Could not process message [%s] with binary data [%b] from endpoint [%s]")
    void errorCouldNotProcessBusMessage(String messageClass, boolean hasBinaryData, String endpoint,
            @Cause Throwable t);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 27, value = "Cannot process an Export JDR response message")
    void errorCannotProcessExportJdrResponseMessage(@Cause Throwable t);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 28, value = "Failed to close WebSocket session [%s] of endpoint [%s] after authentication failure"
            + "of request [%s]")
    void errorCloseSessionAfterAuthFailure(@Cause Throwable t, String sessionId, String endpoint,
            String requestClassName);

    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 29, value = "No WebSocket session of endpoint [%s] found for key [%s] to send a [%s]")
    void warnNoWsSessionForKey(String endpoint, String feedId, String messageClass);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 30, value = "Encountered a message [%s] without a [%s] header in bus endpoint [%s]")
    void errorMessageWithoutFeedId(String messageClass, String header, String endpoint);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 31, value = "Could not invoke [%s.%s] on session [%s] of [%s]")
    void errorInvokingWsSessionListener(String listenerClass, String listenerMethod, String key, String endpoint,
            @Cause Exception e);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 32, value = "Could not close [%s] with selector [%s] for endpoint [%s]")
    void errorCouldNotClose(String closeableClass, String selector, String endpoint, @Cause Throwable e);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 33, value = "Could not add [%s] with selector [%s] to bus endpoint [%s]")
    void errorCouldNotAddBusEndpointListener(String listenerClass, String selector, String endpoint,
            @Cause Throwable e);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 34, value = "%s got a message [%s] without an expected header [%s] value [%s] (expected [%s]) in bus"
            + " endpoint [%s]")
    void errorListenerGotMessageWithUnexpectedHeaderValue(String listenerClass, String messageClass, String header,
            String foundValue, String expectedValue, String endpoint);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 35, value = "Could not destroy [%s] of endpoint [%s]")
    void couldNotDestroy(String name, String endpoint, @Cause Throwable t);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 36, value = "Failed to send error response [%s] to WebSocket client session [%s] of endpoint [%s]")
    void errorFailedToSendErrorResponse(@Cause Throwable t, String name, String id, String endpoint);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 37, value = "Failed sending async binary data to client [%s]")
    void errorFailedSendingAsyncBinaryData(@Cause Throwable t, String id);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 38, value = "Could not close connection context factory [%s]")
    void errorCouldNotCloseConnectionContextFactory(@Cause Throwable t, String name);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 39, value = "Could not initialize [%s]")
    void errorCouldNotInitialize(@Cause Throwable t, String name);

    @Message(id = 40, value = "Failed to lookup [%s] using name [%s] within [%d] ms")
    String errFailedToLookupConnectionFactory(String connectionFactoryClassName, String name, long timeoutMs);

    @Message(id = 41, value = "Failed to lookup [%s] using name [%s]")
    String errFailedToLookupConnectionFactory(String connectionFactoryClassName, String name);
}
