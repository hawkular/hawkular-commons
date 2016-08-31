/*
 * Copyright 2014-2016 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.cmdgw.command.ws;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.hawkular.cmdgw.api.AnsibleRequest;
import org.hawkular.cmdgw.api.AnsibleResponse;
import org.hawkular.cmdgw.command.ws.server.WebSocketHelper;
import org.hawkular.inventory.api.Inventory;
import org.hawkular.inventory.api.ResolvableToSingle;
import org.hawkular.inventory.api.model.DataEntity;
import org.hawkular.inventory.api.model.StructuredData;
import org.hawkular.inventory.paths.CanonicalPath;
import org.hawkular.inventory.paths.SegmentType;

@ApplicationScoped
public class AnsibleCommand implements WsCommand<AnsibleRequest> {


    @javax.annotation.Resource(lookup = "java:global/Hawkular/Inventory")
    private Inventory inventory;

    @Override
    public void execute(BasicMessageWithExtraData<AnsibleRequest> message, WsCommandContext context)
            throws Exception {

        AnsibleRequest ansibleRequest = message.getBasicMessage();

        String rawResourcePath = ansibleRequest.getResourcePath();
        if (rawResourcePath == null) {
            throw new IllegalStateException(ansibleRequest.getClass().getName() + ".resourcePath must not be null");
        }

        CanonicalPath resourcePath = CanonicalPath.fromString(rawResourcePath);
        resourcePath = resourcePath.extend(SegmentType.d, "configuration").get();

        DataEntity data = (DataEntity) inventory.inspect(resourcePath, ResolvableToSingle.class).entity();

        Map<String, StructuredData> configData = data.getValue().map();

        String host = configData.get("Hostname").string();
        Map<String, String> extraVars = ansibleRequest.getExtraVars();
        if (extraVars == null) {
            extraVars = new HashMap<String, String>();
        }
        extraVars.put("wildfly_home_dir", configData.get("Home Directory").string());
        extraVars.put("wildfly_bind_address", configData.get("Bound Address").string());

        // determine what Ansible command line should be executed
        List<String> commandLine = getAnsibleCommandLine(ansibleRequest.getPlaybook(), extraVars, host);

        // build the Ansible command
        ProcessBuilder pb = new ProcessBuilder(commandLine);
        pb.directory(getAnsibleFilesLocation());
        pb.redirectErrorStream(true);

        // run the Ansible command and wait for it to finish
        Process process = pb.start();
        process.waitFor(); // TODO this blocks indefinitely. We'll want to implement a timeout somehow

        // get the results of the Ansible command
        int exitValue = process.exitValue();
        InputStream processOutput = new BufferedInputStream(process.getInputStream());
        String processOutputString = slurpInputStream(processOutput);

        // put the Ansible output in our response
        AnsibleResponse ansibleResponse = new AnsibleResponse();
        ansibleResponse.setResults(String.format("Exit code=[%d]\nOutput:\n%s", exitValue, processOutputString));
        BasicMessageWithExtraData<AnsibleResponse> result = new BasicMessageWithExtraData<>(ansibleResponse, null);

        // send the Ansible response to the client
        new WebSocketHelper().sendSync(context.getSession(), result);
    }

    private List<String> getAnsibleCommandLine(String playbook, Map<String, String> extraVars, String host) throws
            Exception {
        List<String> commandLine = new ArrayList<>();

        commandLine.add("ansible-playbook");
        commandLine.add("-i");
        commandLine.add(host + ",");

        if (extraVars != null) {
            for (Map.Entry<String, String> extraVar : extraVars.entrySet()) {
                commandLine.add("-e");
                commandLine.add(extraVar.getKey() + "=" + extraVar.getValue());
            }
        }

        File playbookFile = getAnsibleFile(playbook);
        commandLine.add(playbookFile.getAbsolutePath());
        return commandLine;
    }

    private String slurpInputStream(InputStream inputStream) throws Exception {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString("UTF-8");
    }

    private File getAnsibleFile(String filename) throws Exception {
        File dir = getAnsibleFilesLocation();
        for (File file : dir.listFiles()) {
            if (file.getName().equals(filename)) {
                return file;
            }
        }
        throw new FileNotFoundException(String.format("Cannot find Ansible file [%s] in [%s]", filename, dir));
    }

    private File getAnsibleFilesLocation() throws Exception {
        String configDirProperty = System.getProperty("jboss.server.config.dir");
        if (configDirProperty == null) {
            throw new FileNotFoundException("Cannot find the location of the Ansible files");
        }

        File ansibleFilesLocation = new File(configDirProperty, "ansible");
        if (!ansibleFilesLocation.isDirectory()) {
            throw new FileNotFoundException("The Ansible files directory is missing: " + ansibleFilesLocation);
        }

        return ansibleFilesLocation;
    }

}
