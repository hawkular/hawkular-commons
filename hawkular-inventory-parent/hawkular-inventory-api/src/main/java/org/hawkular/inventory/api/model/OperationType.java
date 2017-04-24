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

import java.util.Map;
import java.util.Set;

import org.hawkular.inventory.paths.CanonicalPath;
import org.hawkular.inventory.paths.SegmentType;

import io.swagger.annotations.ApiModel;

/**
 * @author Lukas Krejci
 * @since 0.4.0
 */
@ApiModel(description = "Defines an type of operation that can be executed on resources of a resource type" +
        " that contains this operation type. The operation type contains \"returnType\" and \"parameterTypes\"" +
        " data entities which correspond to JSON schemas of values expected during the operation execution.",
        parent = Entity.class)
public final class OperationType extends Entity {

    public static final SegmentType SEGMENT_TYPE = SegmentType.ot;

    @SuppressWarnings("unused")
    private OperationType() {
    }

    public OperationType(CanonicalPath path) {
        super(path);
    }

    public OperationType(String name, CanonicalPath path) {
        super(name, path);
    }

    public OperationType(CanonicalPath path, Map<String, Object> properties) {
        super(path, properties);
    }

    public OperationType(String name, CanonicalPath path, Map<String, Object> properties) {
        super(name, path, properties);
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> visitor, P parameter) {
        return visitor.visitOperationType(this, parameter);
    }

    @ApiModel("OperationTypeBlueprint")
    public static final class Blueprint extends Entity.Blueprint {

        public static Builder builder() {
            return new Builder();
        }

        /**
         * Serialization support
         */
        @SuppressWarnings("unused")
        private Blueprint() {
        }

        public Blueprint(String id, Map<String, Object> properties) {
            super(id, properties);
        }

        public Blueprint(String id, Map<String, Object> properties,
                         Map<String, Set<CanonicalPath>> outgoing,
                         Map<String, Set<CanonicalPath>> incoming) {
            super(id, properties, outgoing, incoming);
        }

        public Blueprint(String id, String name, Map<String, Object> properties,
                         Map<String, Set<CanonicalPath>> outgoing,
                         Map<String, Set<CanonicalPath>> incoming) {
            super(id, name, properties, outgoing, incoming);
        }

        @Override
        public <R, P> R accept(ElementBlueprintVisitor<R, P> visitor, P parameter) {
            return visitor.visitOperationType(this, parameter);
        }

        public static final class Builder extends Entity.Blueprint.Builder<Blueprint, Builder> {
            @Override
            public Blueprint build() {
                return new Blueprint(id, name, properties, outgoing, incoming);
            }
        }
    }
}
