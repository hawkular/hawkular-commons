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
package org.hawkular.inventory.paths;

import java.util.Collection;
import java.util.HashMap;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * An interface a data entity role must implement.
 *
 * <p>Data entity roles are supposed to be enums, but to ensure type safety, we have to have different enums for
 * different entity types. I.e. resources can only have data of roles from the
 * {@link Resource} enum, resource types only from {@link ResourceType} enum and operation types from the
 * {@link OperationType} enum.
 */
@ApiModel(description = "A data role is a string restricted to only certain values depending on what entity contains" +
        " the data with the given role.")
public interface DataRole {

    /**
     * Converts the string representation of the data role to an instance of it. This works across all implementations
     * of DataRole - {@link Resource}, {@link ResourceType} and {@link OperationType}.
     *
     * @param name the name of the data role (one of the values in the above enums)
     * @return the enum instance with given name
     */
    static DataRole valueOf(String name) {
        return DataRole_ImplDetail.instances.get(name);
    }

    /**
     * @return an array of all data roles across all enums implementing the interface -
     * {@link Resource}, {@link ResourceType}, {@link OperationType}.
     */
    static DataRole[] values() {
        Collection<DataRole> values = DataRole_ImplDetail.instances.values();
        return values.toArray(new DataRole[values.size()]);
    }

    /**
     * @return the unique name of the role
     */
    @ApiModelProperty(hidden = true)
    String name();

    /**
     * @return true if this role represents a schema, false otherwise
     */
    @ApiModelProperty(hidden = true)
    boolean isSchema();

    enum Resource implements DataRole {
        configuration, connectionConfiguration;

        @Override
        public boolean isSchema() {
            return false;
        }
    }

    enum ResourceType implements DataRole {
        configurationSchema, connectionConfigurationSchema;

        @Override
        public boolean isSchema() {
            return true;
        }
    }

    enum OperationType implements DataRole {
        returnType, parameterTypes;

        @Override
        public boolean isSchema() {
            return true;
        }
    }
}

/**
 * This is an implementation detail of the {@link DataRole} interface and its implementing enums.
 *
 * <p>Keep this in sync with all the enums that implement the DataRole interface.
 */
//N.B. the name is chosen to lower the chance of class file name collisions if we ever want to have
//another "ImplDetail" class in this package.
final class DataRole_ImplDetail {
    static final HashMap<String, DataRole> instances;

    static {
        instances = new HashMap<>();
        for (DataRole.Resource r : DataRole.Resource.values()) {
            instances.put(r.name(), r);
        }

        for (DataRole.ResourceType r : DataRole.ResourceType.values()) {
            instances.put(r.name(), r);
        }

        for (DataRole.OperationType r : DataRole.OperationType.values()) {
            instances.put(r.name(), r);
        }
    }

    private DataRole_ImplDetail() {

    }
}
