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
package org.hawkular.nest.itest;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;

import org.hawkular.commons.rest.status.RestStatusInfo;

/**
 * For the sake of testing org.hawkular.nest.StatusEndpointITest.
 *
 * @author Lukas Krejci
 * @author <a href="https://github.com/ppalaga">Peter Palaga</a>
 */
@RequestScoped
public class RestStatusDetailsProducer {

    @Produces
    @RestStatusInfo
    public Map<String, String> getRestStatusDetails() throws IOException {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("testKey1", "testValue1");
        return Collections.unmodifiableMap(result);
    }
}