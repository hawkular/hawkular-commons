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
package org.hawkular.commons.email.internal;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.hawkular.commons.email.EmailDispatcher;
import org.hawkular.commons.templates.TemplateService;

import freemarker.template.TemplateException;

/**
 * @author Juraci Paixão Kröhling
 */
@PermitAll
@Singleton
public class EmailDispatcherImpl implements EmailDispatcher {
    MsgLogger logger = MsgLogger.LOGGER;

    @Inject
    TemplateService templateService;

    @Resource(lookup = "java:jboss/mail/Default")
    Session mailSession;

    @Inject @FromAddress
    InternetAddress fromAddress;

    @Override
    public boolean dispatch(InternetAddress recipient,
                            String subject,
                            String templatePlain,
                            String templateHtml,
                            Map<String, Object> properties) {

        Message message = new MimeMessage(mailSession);

        String processedContentPlain;
        String processedContentHtml;
        try {
            processedContentPlain = templateService.getProcessedTemplate(templatePlain, Locale.US, properties);
            processedContentHtml = templateService.getProcessedTemplate(templateHtml, Locale.US, properties);
            message.setFrom(fromAddress);
            message.setRecipient(Message.RecipientType.TO, recipient);
            message.setSubject(subject);

            MimeBodyPart text = new MimeBodyPart();
            text.setContent(processedContentPlain, "text/plain");

            MimeBodyPart rich = new MimeBodyPart();
            rich.setContent(processedContentHtml, "text/html");

            Multipart multipart = new MimeMultipart("alternative");
            multipart.addBodyPart(rich);
            multipart.addBodyPart(text);
            message.setContent(multipart);
        } catch (MessagingException | TemplateException | IOException e) {
            logger.exceptionPreparingMessage(recipient.getAddress(), e);
            return false;
        }

        try {
            Transport.send(message);
        } catch (MessagingException e) {
            logger.exceptionSendingMessage(recipient.getAddress(), e);
            return false;
        }

        return true;
    }
}
