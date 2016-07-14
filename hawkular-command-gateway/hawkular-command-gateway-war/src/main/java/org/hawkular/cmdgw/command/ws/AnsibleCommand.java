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

import java.io.File;
import java.io.FileNotFoundException;

import org.hawkular.bus.common.BasicMessageWithExtraData;
import org.hawkular.cmdgw.api.AnsibleRequest;
import org.hawkular.cmdgw.api.AnsibleResponse;
import org.hawkular.cmdgw.command.ws.server.WebSocketHelper;

public class AnsibleCommand implements WsCommand<AnsibleRequest> {

    @Override
    public void execute(BasicMessageWithExtraData<AnsibleRequest> message, WsCommandContext context)
            throws Exception {
        AnsibleRequest ansibleRequest = message.getBasicMessage();
        String playbook = ansibleRequest.getPlaybook();
        File playbookFile = getAnsiblePlaybook(playbook);

        // return the response
        AnsibleResponse ansibleResponse = new AnsibleResponse();
        ansibleResponse.setResults(String.format("Ansible Playbook [%s] results here!", playbookFile));
        BasicMessageWithExtraData<AnsibleResponse> result = new BasicMessageWithExtraData<>(ansibleResponse, null);
        new WebSocketHelper().sendSync(context.getSession(), result);
    }

    private File getAnsiblePlaybook(String playbook) throws Exception {
        File configDir = new File(System.getProperty("jboss.server.config.dir"));
        for (File file : configDir.listFiles()) {
            if (file.getName().equals(playbook)) {
                return file;
            }
        }
        throw new FileNotFoundException(String.format("Cannot find Ansible playbook [%s]", playbook));
    }

}
