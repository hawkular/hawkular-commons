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

import java.io.IOException;

import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.hawkular.bus.common.BinaryData;
import org.hawkular.cmdgw.api.EchoRequest;
import org.hawkular.cmdgw.api.EchoResponse;
import org.hawkular.cmdgw.command.ws.server.WebSocketHelper;

public class EchoCommand implements WsCommand<EchoRequest> {

    @Override
    public void execute(BasicMessageWithExtraData<EchoRequest> message, WsCommandContext context) throws IOException {
        EchoRequest echoRequest = message.getBasicMessage();
        BinaryData binaryData = message.getBinaryData();
        String echo = String.format("ECHO [%s]", echoRequest.getEchoMessage());

        // return the response
        EchoResponse echoResponse = new EchoResponse();
        echoResponse.setReply(echo);
        BasicMessageWithExtraData<EchoResponse> result = new BasicMessageWithExtraData<>(echoResponse, binaryData);
        new WebSocketHelper().sendSync(context.getSession(), result);
    }
}
