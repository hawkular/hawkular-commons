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
package org.hawkular.cmdgw.command.bus;

import javax.enterprise.context.ApplicationScoped;

import org.hawkular.bus.common.BasicMessage;
import org.hawkular.cmdgw.NoCommandForMessageException;
import org.hawkular.cmdgw.api.UiSessionDestination;

/**
 * A registry of {@link BusCommand}s that can operate on messages coming over a bus queue or topic.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@ApplicationScoped
public class BusCommands {

    private final UiSessionDestinationBusCommand uiSessionDestinationBusCommand = new UiSessionDestinationBusCommand();

    /**
     * Returns a {@link BusCommand} that should handle the given {@code requestClass}.
     *
     * @param requestClass the type of a request for which a processing {@link BusCommand} should be found by this
     *        method
     * @return a {@link BusCommand}, never {@code null}
     * @throws NoCommandForMessageException if no {@link BusCommand} was found
     */
    @SuppressWarnings("unchecked")
    public <REQ extends BasicMessage, RESP extends BasicMessage> BusCommand<REQ> getCommand(Class<REQ> requestClass)
            throws NoCommandForMessageException {
        if (UiSessionDestination.class.isAssignableFrom(requestClass)) {
            return (BusCommand<REQ>) uiSessionDestinationBusCommand;
        }
        // new commands will most probably have to be else-iffed here
        throw new NoCommandForMessageException("No command found for requestClass [" + requestClass.getName() + "]");
    }
}
