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

import org.hawkular.inventory.model.Metric;
import org.hawkular.inventory.model.Resource;
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

    private final Resource resource;

    public IspnResource(Resource resource) {
        if (resource == null) {
            throw new IllegalStateException("Resource must be not null");
        }
        this.feedId = resource.getFeedId();
        this.typeId = resource.getTypeId();
        this.parentId = resource.getParentId();
        this.resource = cloneResource(resource);
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

    public Resource getResource() {
        if (resource == null) {
            return null;
        }
        return cloneResource(resource);
    }

    private Resource cloneResource(Resource resource) {
        if (resource == null) {
            return null;
        }
        Resource.Builder builder = Resource.builder()
                .id(resource.getId())
                .name(resource.getName())
                .feedId(resource.getFeedId())
                .typeId(resource.getTypeId())
                .parentId(resource.getParentId());
        if (resource.getMetrics() != null) {
            for (Metric metric : resource.getMetrics()) {
                // TODO [lponce] clone metric ?
                builder.metric(metric);
            }
        }
        if (resource.getProperties() != null) {
            // TODO [lponce] clone properties ?
            builder.properties(resource.getProperties());
        }
        return builder.build();
    }
}
