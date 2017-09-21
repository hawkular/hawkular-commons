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
package org.hawkular.inventory.api;

import java.io.Serializable;
import java.util.List;

import org.hawkular.inventory.model.Metric;
import org.hawkular.inventory.model.Resource;
import org.hawkular.inventory.model.ResourceType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class Import implements Serializable {

    @JsonInclude(Include.NON_NULL)
    private List<Resource> resources;

    @JsonInclude(Include.NON_NULL)
    private List<ResourceType> types;

    @JsonInclude(Include.NON_NULL)
    private List<Metric> metrics;

    public Import(@JsonProperty("resources") List<Resource> resources,
                  @JsonProperty("types") List<ResourceType> types,
                  @JsonProperty("metrics") List<Metric> metrics) {
        this.resources = resources;
        this.types = types;
        this.metrics = metrics;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

    public List<ResourceType> getTypes() {
        return types;
    }

    public void setTypes(List<ResourceType> types) {
        this.types = types;
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<Metric> metrics) {
        this.metrics = metrics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Import anImport = (Import) o;

        if (resources != null ? !resources.equals(anImport.resources) : anImport.resources != null) return false;
        if (types != null ? !types.equals(anImport.types) : anImport.types != null) return false;
        return metrics != null ? metrics.equals(anImport.metrics) : anImport.metrics == null;
    }

    @Override
    public int hashCode() {
        int result = resources != null ? resources.hashCode() : 0;
        result = 31 * result + (types != null ? types.hashCode() : 0);
        result = 31 * result + (metrics != null ? metrics.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Import{" +
                "resources=" + resources +
                ", types=" + types +
                ", metrics=" + metrics +
                '}';
    }
}
