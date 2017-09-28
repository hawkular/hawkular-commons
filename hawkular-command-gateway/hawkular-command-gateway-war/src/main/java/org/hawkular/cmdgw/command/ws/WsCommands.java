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

import java.util.ArrayList;
import java.util.Collection;

import javax.enterprise.context.ApplicationScoped;

import org.hawkular.bus.common.BasicMessage;
import org.hawkular.cmdgw.NoCommandForMessageException;
import org.hawkular.cmdgw.api.EchoRequest;
import org.hawkular.cmdgw.api.EventDestination;
import org.hawkular.cmdgw.api.ResourceDestination;
import org.hawkular.cmdgw.api.UiSessionDestination;

/**
 * A registry of {@link WsCommand} that can operate on messages coming over a WebSocket.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@ApplicationScoped
public class WsCommands {

    // Here we define the only known commands we are expected to handle.
    // Notice we instantiate them here - these must be thread-safe.

    private final ResourceDestinationWsCommand resourcePathDestinationWsCommand = //
    new ResourceDestinationWsCommand();

    private final EchoCommand echoCommand = new EchoCommand();
    private final UiSessionDestinationWsCommand uiSessionDestinationWsCommand = new UiSessionDestinationWsCommand();
    private final EventDestinationWsCommand eventDestinationWsCommand = new EventDestinationWsCommand();

    /**
     * Returns a collection of {@link WsCommand}s that should handle the given {@code requestClass}.
     *
     * @param requestClass the type of a request for which a processing {@link WsCommand} should be found by this method
     * @return a collection of {@link WsCommand} objects, never {@code null}
     * @throws NoCommandForMessageException if no {@link WsCommand} was found
     */
    @SuppressWarnings("unchecked")
    public <REQ extends BasicMessage> Collection<WsCommand<REQ>> getCommands(Class<REQ> requestClass)
            throws NoCommandForMessageException {

        ArrayList<WsCommand<REQ>> results = new ArrayList<>(2);

        if (ResourceDestination.class.isAssignableFrom(requestClass)) {
            results.add((WsCommand<REQ>) resourcePathDestinationWsCommand);
        } else if (UiSessionDestination.class.isAssignableFrom(requestClass)) {
            results.add((WsCommand<REQ>) uiSessionDestinationWsCommand);
        } else if (EchoRequest.class.isAssignableFrom(requestClass)) {
            results.add((WsCommand<REQ>) echoCommand);
        }

        // some commands may also want the message sent to events
        if (EventDestination.class.isAssignableFrom(requestClass)) {
            results.add((WsCommand<REQ>) eventDestinationWsCommand);
        }

        // new commands will need to be added here

        if (results.isEmpty()) {
            throw new NoCommandForMessageException(
                    "No command found for requestClass [" + requestClass.getName() + "]");
        }

        return results;
    }
}
