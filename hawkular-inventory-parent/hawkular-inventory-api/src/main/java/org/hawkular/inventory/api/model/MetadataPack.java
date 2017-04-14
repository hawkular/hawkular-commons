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

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hawkular.inventory.paths.CanonicalPath;
import org.hawkular.inventory.paths.SegmentType;

import io.swagger.annotations.ApiModel;

/**
 * A metadata pack incorporates a bunch of resource types and metric types. It computes a hash of its "contents" so that
 * merely by examining the hash, one can make sure that certain set of resource types and metric types is present in
 * the form one expects.
 *
 * @author Lukas Krejci
 * @since 0.7.0
 */
@ApiModel(description = "A metadata pack can incorporate global resource and metric types making them read-only.",
        parent = Entity.class)
public final class MetadataPack extends Entity {

    public static final SegmentType SEGMENT_TYPE = SegmentType.mp;

    public static boolean canIncorporate(CanonicalPath entityPath) {
        SegmentType entityType = entityPath.getSegment().getElementType();
        SegmentType parentType = entityPath.up().getSegment().getElementType();

        return SegmentType.t.equals(parentType)
                && (SegmentType.rt.equals(entityType) || SegmentType.mt.equals(entityType));
    }

    public MetadataPack(String name, CanonicalPath path, Map<String, Object> properties) {
        super(name, path, properties);
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> visitor, P parameter) {
        return visitor.visitMetadataPack(this, parameter);
    }

    @ApiModel("MetadataPackBlueprint")
    public static final class Blueprint extends AbstractElement.Blueprint {

        private final Set<CanonicalPath> members;
        private final String name;

        public static Builder builder() {
            return new Builder();
        }

        public Blueprint(String name, Set<CanonicalPath> members, Map<String, Object> properties) {
            super(properties);

            this.name = name;

            members.forEach((p) -> {
                if (!canIncorporate(p)) {
                    throw new IllegalArgumentException("Entity on path '" + p + "' cannot be part of a metadata pack.");
                }
            });

            this.members = Collections.unmodifiableSet(new HashSet<>(members));
        }

        public String getName() {
            return name;
        }

        @Override
        public <R, P> R accept(ElementBlueprintVisitor<R, P> visitor, P parameter) {
            return visitor.visitMetadataPack(this, parameter);
        }

        public static final class Builder extends Entity.Blueprint.Builder<Blueprint, Builder> {
            private final Set<CanonicalPath> members = new HashSet<>();
            private String name;

            public Builder withName(String name) {
                this.name = name;
                return this;
            }

            @Override
            public Blueprint build() {
                return new Blueprint(name, members, properties);
            }
        }
    }
}
