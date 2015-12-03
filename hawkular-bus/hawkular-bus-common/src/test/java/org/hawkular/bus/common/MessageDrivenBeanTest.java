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
package org.hawkular.bus.common;

import static org.junit.Assert.assertEquals;

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.Queue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author jsanda
 */
@RunWith(Arquillian.class)
public class MessageDrivenBeanTest {

    @Deployment
    public static JavaArchive createDeployment() {
        JavaArchive archive = ShrinkWrap.create(JavaArchive.class)
                .addPackages(true, "org.hawkular.bus")
//                .addAsManifestResource(MessageDrivenBeanTest.class.getResource("/MANIFEST.MF"), "MANIFEST.MF")
                .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
//        ZipExporter exporter = new ZipExporterImpl(archive);
//        exporter.exportTo(new File("target", "test-archive.jar"));
        return archive;
    }

    @Resource(mappedName = "java:/jms/queue/TestQueue")
    Queue testQueue;

    @Inject
    Bus bus;

    @Test
    public void sendMessage() throws Exception {
        BasicTextMessage message = new BasicTextMessage("test");
        BasicTextMessage reply = bus.sendAndReceive(testQueue, message, 1000);

        assertEquals(message.getText().toUpperCase(), reply.getText());
    }

}
