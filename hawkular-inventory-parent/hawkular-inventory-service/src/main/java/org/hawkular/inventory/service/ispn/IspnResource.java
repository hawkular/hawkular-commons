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
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.hawkular.inventory.api.model.RawResource;
import org.hawkular.inventory.api.model.Resource;
import org.hawkular.inventory.api.model.ResourceNode;
import org.hawkular.inventory.api.model.ResourceType;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@Indexed(index = "resource")
public class IspnResource implements Serializable {

    @Field(store = Store.YES, analyze = Analyze.NO, indexNullAs = Field.DEFAULT_NULL_TOKEN)
    private final String feedId;

    @Field(store = Store.YES, analyze = Analyze.NO, indexNullAs = Field.DEFAULT_NULL_TOKEN)
    private final String typeId;

    @Field(store = Store.YES, analyze = Analyze.NO, indexNullAs = Field.DEFAULT_NULL_TOKEN)
    private final String parentId;

    private final RawResource rawResource;

    public IspnResource(RawResource resource) {
        if (resource == null) {
            throw new IllegalStateException("Resource must be not null");
        }
        this.feedId = resource.getFeedId();
        this.typeId = resource.getTypeId();
        this.parentId = resource.getParentId();
        this.rawResource = resource;
    }

    public String getFeedId() {
        return feedId;
    }

    public String getTypeId() {
        return typeId;
    }

    public String getParentId() {
        return parentId;
    }

    public RawResource getRawResource() {
        return rawResource;
    }

    public Resource toResource(Function<String, Optional<ResourceType>> rtLoader) {
        return Resource.fromRaw(rawResource, rtLoader);
    }

    public ResourceNode toResourceNode(Function<String, Optional<ResourceType>> rtLoader,
                                       Function<String, List<RawResource>> rLoader) {
        return ResourceNode.fromRaw(rawResource, rtLoader, rLoader);
    }
}
