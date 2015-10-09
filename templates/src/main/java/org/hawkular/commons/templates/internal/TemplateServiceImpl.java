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
package org.hawkular.commons.templates.internal;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.security.PermitAll;
import javax.ejb.Singleton;
import javax.inject.Inject;

import org.hawkular.commons.templates.TemplateService;

import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * @author Juraci Paixão Kröhling
 */
@PermitAll
@Singleton
public class TemplateServiceImpl implements TemplateService {
    @Inject @OverrideDirectory
    String overrideDirectory;

    @Inject @ConfigurationDirectory
    String configurationDirectory;

    private Configuration configuration = new Configuration(Configuration.VERSION_2_3_23);

    public TemplateServiceImpl() throws IOException {
        this(null, null);
    }

    public TemplateServiceImpl(String overrideDirectory, String configurationDirectory) throws IOException {
        if (null != overrideDirectory) {
            this.overrideDirectory = overrideDirectory;
        }

        if (null != configurationDirectory) {
            this.configurationDirectory = configurationDirectory;
        }
    }

    @Override
    public String getProcessedTemplate(String templateName, Locale locale, Map<String, Object> properties)
            throws IOException, TemplateException {
        Template template = configuration.getTemplate(templateName, locale);
        StringWriter writer = new StringWriter();
        template.process(properties, writer);
        return writer.toString();
    }

    @PostConstruct
    void configure() throws IOException {
        Set<TemplateLoader> loaders = new LinkedHashSet<>();
        if (null != overrideDirectory) {
            loaders.add(new FileTemplateLoader(new File(this.overrideDirectory)));
        }

        if (null != configurationDirectory) {
            loaders.add(new FileTemplateLoader(new File(this.configurationDirectory)));
        }

        MultiTemplateLoader mtl = new MultiTemplateLoader(loaders.toArray(new TemplateLoader[loaders.size()]));
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateLoader(mtl);
    }
}
