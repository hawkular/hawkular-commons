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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import freemarker.template.TemplateException;

/**
 * @author Juraci Paixão Kröhling
 */
public class TemplateServiceImplTest {
    TemplateServiceImpl service;

    private File configDir;
    private File overrideDir;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void prepare() throws IOException {
        configDir = temporaryFolder.newFolder("configuration");
        overrideDir = temporaryFolder.newFolder("override");
        service = new TemplateServiceImpl(overrideDir.getAbsolutePath(), configDir.getAbsolutePath());
        service.configure();
    }

    @Test
    public void simplestCase() throws IOException, TemplateException {
        File createdTemplateFile = new File(configDir.getAbsolutePath() + "/invitation.ftl");
        if (!createdTemplateFile.createNewFile()) {
            fail("Could not create temporary file for the test");
        }
        FileWriter writer = new FileWriter(createdTemplateFile);
        writer.write("Hello ${name}");
        writer.close();

        Map<String, Object> properties = new HashMap<>(1);
        properties.put("name", "World");

        String processed = service.getProcessedTemplate("invitation.ftl", null, properties);
        assertEquals("The template should have been processed", "Hello World", processed);
    }

    @Test
    public void simplestCaseWithLocale() throws IOException, TemplateException {
        File createdTemplateFile = new File(configDir.getAbsolutePath() + "/invitation_en_US.ftl");
        if (!createdTemplateFile.createNewFile()) {
            fail("Could not create temporary file for the test");
        }
        FileWriter writer = new FileWriter(createdTemplateFile);
        writer.write("Hello ${name}");
        writer.close();

        Map<String, Object> properties = new HashMap<>(1);
        properties.put("name", "World");

        String processed = service.getProcessedTemplate("invitation.ftl", Locale.US, properties);
        assertEquals("The template should have been processed", "Hello World", processed);
    }
}
