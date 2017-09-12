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

import java.util.EnumMap;

import org.hawkular.inventory.api.oldmodel.AbstractElement;
import org.hawkular.inventory.api.oldmodel.Blueprint;
import org.hawkular.inventory.api.oldmodel.DataEntity;
import org.hawkular.inventory.api.oldmodel.Environment;
import org.hawkular.inventory.api.oldmodel.Feed;
import org.hawkular.inventory.api.oldmodel.MetadataPack;
import org.hawkular.inventory.api.oldmodel.Metric;
import org.hawkular.inventory.api.oldmodel.MetricType;
import org.hawkular.inventory.api.oldmodel.OperationType;
import org.hawkular.inventory.api.oldmodel.Relationship;
import org.hawkular.inventory.api.oldmodel.Resource;
import org.hawkular.inventory.api.oldmodel.ResourceType;
import org.hawkular.inventory.api.oldmodel.Tenant;
import org.hawkular.inventory.paths.SegmentType;

/**
 * Inventory stores "resources" which are groupings of measurements and other data. Inventory also stores metadata about
 * the measurements and resources to give them meaning.
 *
 * <p>The resources are organized by tenant (your company) and environments (i.e. testing, development, staging,
 * production, ...).
 *
 * <p>Despite their name, tenants are not completely separated and one can easily create relationships between them or
 * between entities underneath different tenants. This is because there are situations where such relationships might
 * make sense but more importantly because at the API level, inventory does not mandate any security model. It is
 * assumed that the true multi-tenancy in the common sense of the word is implemented by a layer on top of the inventory
 * API that also understands some security model to separate the tenants.
 *
 * <p>Resources are hierarchical - meaning that one can be a parent of others, recursively. One can also say that a
 * resource can contain other resources. Resources can have other kinds of relationships that are not necessarily
 * tree-like.
 *
 * <p>Resources can have a "resource type" (but they don't have to) which prescribes what kind of data a resource
 * contains. Most prominently a resource can have a list of metrics and a resource type can define what those metrics
 * should be by specifying the set of "metric types".
 *
 * <p>This interface offers a fluent API to compose a "traversal" over the graph of entities stored in the inventory in
 * a strongly typed fashion.
 *
 * <p>The inventory implementations are not required to be thread-safe. Instances should therefore be accessed only by a
 * single thread or serially.
 *
 * @author Lukas Krejci
 * @since 0.0.1
 */
public interface Inventory {

    /**
     * @return a registry of various types associated with entities
     */
    static Types types() {
        return Types.INSTANCE;
    }

    /**
     * A registry of various types used with entities. You can look up an by things like segment type, entity type,
     * blueprint type, etc. and then obtain the rest of the types for the corresponding entity type.
     */
    @SuppressWarnings("unchecked")
    final class Types {
        private static final Types INSTANCE = new Types();
        private static final EnumMap<SegmentType, ElementTypes<?, ?>> elementTypes;
        static {
            elementTypes = new EnumMap<>(SegmentType.class);

            elementTypes.put(SegmentType.d,
                    new ElementTypes<>((Class) DataEntity.Blueprint.class,
                            DataEntity.class, SegmentType.d));
            elementTypes.put(SegmentType.e,
                    new ElementTypes<>(Environment.Blueprint.class, Environment.class, SegmentType.e));
            elementTypes.put(SegmentType.f,
                    new ElementTypes<>(Feed.Blueprint.class, Feed.class, SegmentType.f));
            elementTypes.put(SegmentType.m,
                    new ElementTypes<>(Metric.Blueprint.class, Metric.class, SegmentType.m));
            elementTypes.put(SegmentType.mp,
                    new ElementTypes<>(MetadataPack.Blueprint.class, MetadataPack.class, SegmentType.mp));
            elementTypes.put(SegmentType.mt,
                    new ElementTypes<>(MetricType.Blueprint.class, MetricType.class, SegmentType.mt));
            elementTypes.put(SegmentType.ot,
                    new ElementTypes<>(OperationType.Blueprint.class, OperationType.class, SegmentType.ot));
            elementTypes.put(SegmentType.r,
                    new ElementTypes<>(Resource.Blueprint.class, Resource.class, SegmentType.r));
            elementTypes.put(SegmentType.rl,
                    new ElementTypes<>(Relationship.Blueprint.class, Relationship.class, SegmentType.rl));
            elementTypes.put(SegmentType.rt,
                    new ElementTypes<>(ResourceType.Blueprint.class, ResourceType.class, SegmentType.rt));
            elementTypes.put(SegmentType.t,
                    new ElementTypes<>(Tenant.Blueprint.class, Tenant.class, SegmentType.t));
        }

        private Types() {
        }

        public <B extends Blueprint> ElementTypes<? extends AbstractElement, B>
        byBlueprint(Class<B> blueprintType) {
            for(SegmentType st : SegmentType.values()) {
                ElementTypes ret = elementTypes.get(st);
                if (ret.getBlueprintType().equals(blueprintType)) {
                    return ret;
                }
            }

            throw new IllegalArgumentException("Unknown blueprint type: " + blueprintType);
        }

        public <E extends AbstractElement, B extends Blueprint>
        ElementTypes<E, B> byElement(Class<E> elementType) {
            for(SegmentType st : SegmentType.values()) {
                ElementTypes ret = elementTypes.get(st);
                if (ret.getElementType().equals(elementType)) {
                    return ret;
                }
            }

            throw new IllegalArgumentException("Unknown element type: " + elementType);
        }
    }

    final class ElementTypes<E extends AbstractElement, B extends Blueprint> {
        private final Class<B> blueprintType;
        private final Class<E> elementType;
        private final SegmentType segmentType;

        private ElementTypes(Class<B> blueprintType, Class<E> elementType, SegmentType segmentType) {
            this.blueprintType = blueprintType;
            this.elementType = elementType;
            this.segmentType = segmentType;
        }

        public Class<B> getBlueprintType() {
            return blueprintType;
        }

        public Class<E> getElementType() {
            return elementType;
        }

        public SegmentType getSegmentType() {
            return segmentType;
        }
    }
}
