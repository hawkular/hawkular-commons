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
package org.hawkular.inventory.api.model;

import java.io.Serializable;
import java.util.List;

import org.hawkular.commons.doc.DocModel;
import org.hawkular.commons.doc.DocModelProperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@DocModel(description = "Representation of a list of resources and a list of resource types. + \n" +
        "Used for bulk import/export operations.")
public class Inventory implements Serializable {

    @DocModelProperty(description = "List of resources.",
            position = 0,
            required = false)
    @JsonInclude(Include.NON_NULL)
    private List<RawResource> resources;

    @DocModelProperty(description = "List of resource types.",
            position = 1,
            required = false)
    @JsonInclude(Include.NON_NULL)
    private List<ResourceType> types;

    public Inventory(@JsonProperty("resources") List<RawResource> resources,
                     @JsonProperty("types") List<ResourceType> types) {
        this.resources = resources;
        this.types = types;
    }

    public List<RawResource> getResources() {
        return resources;
    }

    public void setResources(List<RawResource> resources) {
        this.resources = resources;
    }

    public List<ResourceType> getTypes() {
        return types;
    }

    public void setTypes(List<ResourceType> types) {
        this.types = types;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Inventory anInventory = (Inventory) o;

        if (resources != null ? !resources.equals(anInventory.resources) : anInventory.resources != null) return false;
        return types != null ? types.equals(anInventory.types) : anInventory.types == null;
    }

    @Override
    public int hashCode() {
        int result = resources != null ? resources.hashCode() : 0;
        result = 31 * result + (types != null ? types.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Inventory{" +
                "resources=" + resources +
                ", types=" + types +
                '}';
    }
}
