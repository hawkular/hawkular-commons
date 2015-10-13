/*
 * Copyright 2014-2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.commons.email;

import java.util.Map;

import javax.mail.internet.InternetAddress;

/**
 * @author Juraci Paixão Kröhling
 */
public interface EmailDispatcher {

    /**
     * Dispatches an email message to the given recipient, with the subject. The template parameters and the
     * properties are for the Freemarker formatting, and should follow its standards. For instance, if templatePlain
     * is 'invitation_plain.ftl' then 'invitation_plain.ftl' would be loaded from one of the template repositories (or
     * invitation_plain_$LOCALE.ftl, according to the Freemarker rules of template loading). The properties map is a
     * set of properties to be merged with the templates.
     *
     * @param recipient        the recipient of the message
     * @param subject          the subject of the message, already localized
     * @param templatePlain    the template to use for plain text
     * @param templateHtml     the template to use for HTML message
     * @param properties       the properties to merge with the template
     * @return  a boolean indicating whether the message was dispatched
     */
    boolean dispatch(InternetAddress recipient,
                     String subject,
                     String templatePlain,
                     String templateHtml,
                     Map<String, Object> properties);

}
