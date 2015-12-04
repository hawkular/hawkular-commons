<%--

    Copyright 2015 Red Hat, Inc. and/or its affiliates
    and other contributors as indicated by the @author tags.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

--%>
<html>
    <head>
        <title> Simple App Level JMS Virtual Topic Demo </title>
    </head>
    <body>
        <form action="VirtualTopicSendServlet">
            Fire-n-Forget: Enter Message to Send:<br/>
            <textarea name="jmsMessageFNF"cols="20" rows="1">Enter JMS Message</textarea><br/>
            <input type="Submit" value="Send JMS Message" />
            <input type="Reset" value="Clear" />
        </form>
    </body>
</html>
