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

import java.io.UnsupportedEncodingException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.mail.internet.InternetAddress;

/**
 * @author Juraci Paixão Kröhling
 */
@ApplicationScoped
public class ApplicationResources {
    @ApplicationScoped @Produces @FromAddress
    public InternetAddress produceEmailFromAddress() throws UnsupportedEncodingException {
        String fromAddress = System.getProperty("org.hawkular.email.from.address", "noreply@hawkular.org");
        String fromName = System.getProperty("org.hawkular.email.from.name", "Hawkular");

        return new InternetAddress(fromAddress, fromName);
    }
}
