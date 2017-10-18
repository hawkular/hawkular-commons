/*
 * Copyright 2014-2017 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.client.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Jay Shaughnessy
 */
public enum NotificationType {
    RESOURCE_ADDED("resourceType", "feedId", "resourceId"),
    AVAIL_STARTING("resourceType", "feedId", "resourceId", "availType", "newAvail"),
    AVAIL_CHANGE("resourceType", "feedId", "resourceId", "availType", "newAvail");

    private List<String> supportedProperties;

    NotificationType(String... supportedProperties) {
        this.supportedProperties = Collections.unmodifiableList(Arrays.asList(supportedProperties));
    }

    public List<String> getSupportedProperties() {
        return supportedProperties;
    }
}
