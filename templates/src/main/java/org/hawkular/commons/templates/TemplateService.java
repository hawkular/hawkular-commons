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
package org.hawkular.commons.templates;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;

import freemarker.template.TemplateException;

/**
 * @author Juraci Paixão Kröhling
 */
public interface TemplateService {

    /**
     * Merges the given template name with the properties. Uses the given locale to determine which template to load,
     * but it's not determinant, ie, if a specific template for the given locale doesn't exists, it loads the default
     * template. If no such template is found, a TemplateException is thrown.
     *
     * @param templateName    the template name, such as 'invitation_plain.ftl'.
     * @param locale          the locale for the template, such as Locale.US
     * @param properties      the properties to merge the template with
     * @return  a String with the merged template
     */
    String getProcessedTemplate(String templateName, Locale locale, Map<String, Object> properties)
            throws IOException, TemplateException;
}
