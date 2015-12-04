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
package org.hawkular.bus.sample.mdb;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.jms.TopicConnectionFactory;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hawkular.bus.common.ConnectionContextFactory;
import org.hawkular.bus.common.Endpoint;
import org.hawkular.bus.common.MessageId;
import org.hawkular.bus.common.MessageProcessor;
import org.hawkular.bus.common.SimpleBasicMessage;
import org.hawkular.bus.common.producer.ProducerConnectionContext;

public class VirtualTopicSendServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String CONN_FACTORY = "/HawkularBusConnectionFactory";

    // the full name is "java:/topic/VirtualTopic.ExampleName"
    private static final String TOPIC_NAME = "VirtualTopic.ExampleName";

    private static final Map<String, String> FNF_HEADER = createMyFilterHeader("fnf");

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userMessage = request.getParameter("jmsMessageFNF");
        if (userMessage != null) {
            fireAndForget(request, response, userMessage);
        } else {
            throw new ServletException("Don't know what to send!");
        }
    }

    protected void fireAndForget(HttpServletRequest request, HttpServletResponse response, String userMessage) {
        try {
            InitialContext ctx = new InitialContext();
            TopicConnectionFactory qconFactory = (TopicConnectionFactory) ctx.lookup(CONN_FACTORY);

            ConnectionContextFactory ccf = new ConnectionContextFactory(qconFactory);
            ProducerConnectionContext pcc = ccf.createProducerConnectionContext(new Endpoint(Endpoint.Type.TOPIC,
                    TOPIC_NAME));

            SimpleBasicMessage msg = new SimpleBasicMessage(userMessage);
            MessageId mid = new MessageProcessor().send(pcc, msg, FNF_HEADER);

            PrintWriter out = response.getWriter();
            out.println("<h1>Fire and Forget</h1>");
            out.println("<p>BasicMessage Sent [" + msg + "]</p>");
            out.println("<p>(messageId=" + mid + ")</p>");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // return the header that our sample MDBs' selectors will look at
    private static Map<String, String> createMyFilterHeader(String value) {
        Map<String, String> map = new HashMap<String, String>(1);
        map.put("MyFilter", value);
        return map;
    }
}
