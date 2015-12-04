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
package org.hawkular.bus.common.consumer;

import javax.jms.Message;

/**
 * This is a context that will be associated with an incoming RPC call (that is, a request is sent and a response is
 * expected back; mimicing an RPC call).
 *
 * This context includes the consumer data that will be used to process the incoming request message, including the
 * {@link #getDestination() destination} where the response is going to be sent, as well as the request message itself
 * (which in turn will have the request MessageId once the request is sent) and the listener that will process the
 * response.
 *
 * This object is used on the receiving end - that is, the consumer end that accepts the request.
 *
 * @author John Mazzitelli
 */
public class RPCConnectionContext extends ConsumerConnectionContext {
    private Message requestMessage;
    private BasicMessageListener<?> responseListener;

    /**
     * This is the request message that was sent. A response is expected from this request message. When the message is
     * successfully sent, it will have its message ID set.
     *
     * @return the request message
     */
    public Message getRequestMessage() {
        return requestMessage;
    }

    public void setRequestMessage(Message requestMessage) {
        this.requestMessage = requestMessage;
    }

    /**
     * This is the listener that is assigned to process the returned response.
     *
     * @return listener
     */
    public BasicMessageListener<?> getResponseListener() {
        return responseListener;
    }

    public void setResponseListener(BasicMessageListener<?> responseListener) {
        this.responseListener = responseListener;
    }
}
