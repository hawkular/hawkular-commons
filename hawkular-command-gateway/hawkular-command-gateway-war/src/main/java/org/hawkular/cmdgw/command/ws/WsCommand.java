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

import org.hawkular.bus.common.BasicMessage;
import org.hawkular.bus.common.BasicMessageWithExtraData;

/**
 * An command to execute on messages coming over a WebSocket.
 * <p>
 * Note on thread safety: implementations of this interface must not store any state outside of the
 * {@link #execute(BasicMessageWithExtraData, WsCommandContext)} method so that the instances can be safely shared
 * between multiple threads.
 *
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 * @param <M> the type of the message to process
 */
public interface WsCommand<M extends BasicMessage> {

    /**
     * Performs the command for the given {@code message}.
     *
     * @param message the request to execute
     * @param context some context data that can be useful for the command to be able to execute the request
     * @throws Exception if failed to execute the operation
     */
    void execute(BasicMessageWithExtraData<M> message, WsCommandContext context) throws Exception;
}
