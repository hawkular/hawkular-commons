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
package org.hawkular.inventory.service;

import java.util.Map;

import org.hawkular.inventory.api.model.RawResource;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class ScrapeConfig {

    @JsonInclude(Include.NON_NULL)
    private Map<String, String> filter;

    public ScrapeConfig(@JsonProperty("filter") Map<String, String> filter) {
        this.filter = filter;
    }

    public Map<String, String> getFilter() {
        return filter;
    }

    public boolean filter(RawResource resource) {
        if (resource == null || resource.getTypeId() == null) {
            return false;
        }
        return filter.containsKey(resource.getTypeId());
    }

    @Override
    public String toString() {
        return "ScrapeConfig{" +
                "filter=" + filter +
                '}';
    }
}
