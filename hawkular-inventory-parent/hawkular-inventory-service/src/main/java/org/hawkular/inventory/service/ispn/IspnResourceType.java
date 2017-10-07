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
package org.hawkular.inventory.service.ispn;

import java.io.Serializable;

import org.hawkular.inventory.model.Operation;
import org.hawkular.inventory.model.ResourceType;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@Indexed(index = "resourceType")
public class IspnResourceType implements Serializable {

    @Field(store = Store.YES, analyze = Analyze.NO, indexNullAs = Field.DEFAULT_NULL_TOKEN)
    private final String id;  // Unique index [Search resource type by id]

    private final ResourceType resourceType;

    public IspnResourceType(ResourceType resourceType) {
        if (resourceType == null) {
            throw new IllegalStateException("ResourceType must be not null");
        }
        this.id = resourceType.getId();
        this.resourceType = cloneResourceType(resourceType);
    }

    public String getId() {
        return id;
    }

    public ResourceType getResourceType() {
        if (resourceType == null) {
            return null;
        }
        return cloneResourceType(resourceType);
    }

    private ResourceType cloneResourceType(ResourceType resourceType) {
        if (resourceType == null) {
            return null;
        }
        ResourceType.Builder builder = ResourceType.builder()
                .id(resourceType.getId())
                .properties(resourceType.getProperties());
        if (resourceType.getOperations() != null) {
            for (Operation operation : resourceType.getOperations()) {
                builder.operation(operation);
            }
        }
        return builder.build();
    }
}
