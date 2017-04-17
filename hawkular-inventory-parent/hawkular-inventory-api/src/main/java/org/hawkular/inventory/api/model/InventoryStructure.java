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

import static org.hawkular.inventory.api.model.Helper.ENTITY_ORDER;
import static org.hawkular.inventory.paths.SegmentType.r;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hawkular.inventory.api.Inventory;
import org.hawkular.inventory.paths.ElementTypeVisitor;
import org.hawkular.inventory.paths.Path;
import org.hawkular.inventory.paths.RelativePath;
import org.hawkular.inventory.paths.SegmentType;

import io.swagger.annotations.ApiModel;

/**
 * Represents the structure of an inventory. It is supposed that the structure is loaded lazily. The structure is
 * represented using entity blueprints instead of entity types themselves so that this structure can be computed
 * offline, without access to all information up to the tenant.
 *
 * @author Lukas Krejci
 * @since 0.11.0
 */
public interface InventoryStructure<Root extends Entity.Blueprint> {

    /**
     * Shortcut method, exactly identical to calling {@link Offline#of(Entity.Blueprint)}.
     *
     * @param root the root blueprint
     * @param <B> the type of the blueprint
     * @return the builder to build an offline inventory structure
     */
    static <B extends Entity.Blueprint> InventoryStructure.Offline.Builder<B> of(B root) {
        return of(root, null);
    }

    /**
     * Shortcut method, exactly identical to calling {@link Offline#of(Entity.Blueprint, Object)}.
     *
     * @param root the root blueprint
     * @param attachment the attachment of the blueprint
     * @param <B> the type of the blueprint
     * @return the builder to build an offline inventory structure
     */
    static <B extends Entity.Blueprint> InventoryStructure.Offline.Builder<B> of(B root, Object attachment) {
        return Offline.of(root, attachment);
    }

    /**
     * @return the root entity
     */
    Root getRoot();


    /**
     * Returns the direct children of given type under the supplied path to the parent entity, which is relative to some
     * root entity for which this structure was instantiated.
     *
     * <p><b>WARNING</b>: the returned stream MUST BE closed after processing.
     *
     * @param parent    the path to the parent entity, relative to the root entity
     * @param childType the type of the child entities to retrieve
     * @param <E>       the type of the child entities
     * @param <B>       the type of the child entity blueprint
     * @return a stream of blueprints corresponding to the child entities.
     */
    <E extends Entity, B extends Entity.Blueprint> Stream<B> getChildren(RelativePath parent,
                                                                                  Class<E> childType);

    @SuppressWarnings({"unchecked", "rawtypes"})
    default <E extends Entity, B extends Entity.Blueprint>
    Stream<FullNode> getChildNodes(RelativePath parent, Class<E> childType) {
        SegmentType childSegmentType = Inventory.types().byElement((Class)childType).getSegmentType();
        return getChildren(parent, childType).map(b -> {
            RelativePath childPath = parent.modified().extend(childSegmentType, b.getId()).get();
            return getNode(childPath);
        }).filter(Objects::nonNull);
    }

    /**
     * Gets a blueprint on the given path.
     *
     * @param path the path under the root of the structure
     * @return the blueprint describing the entity on the given path
     */
    Entity.Blueprint get(RelativePath path);

    /**
     * By providing an empty relative path, one can retrieve the attachment of the root entity.
     * @param path the path to the entity in the inventory structure
     * @return the object attached to the entity by the inventory structure builder or null if the path is not known
     */
    default FullNode getNode(RelativePath path) {
        Entity.Blueprint bl = get(path);
        return bl == null ? null : new FullNode(bl, null);
    }

    /**
     * Returns all direct children of the specified parent.
     *
     * <b>WARNING</b>: the returned stream MUST BE closed after processing.
     *
     * @param parent the parent of which to return the children
     * @return the stream of all children of the parent
     */
    default Stream<Entity.Blueprint> getAllChildren(RelativePath parent) {
        return getAllChildNodes(parent).map(FullNode::getEntity);
    }

    /**
     * Returns all direct children of the specified parent.
     *
     * <b>WARNING</b>: the returned stream MUST BE closed after processing.
     *
     * @param parent the parent of which to return the children
     * @return the stream of all children of the parent
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    default Stream<FullNode> getAllChildNodes(RelativePath parent) {
        Stream<FullNode> ret = Stream.empty();
        RelativePath.Extender check = RelativePath.empty()
                .extend(Blueprint.getSegmentTypeOf(getRoot()),getRoot().getId())
                .extend(parent.getPath());

        for (EntityType et : EntityType.values()) {
            SegmentType st = et.segmentType;
            if (check.canExtendTo(st)) {
                List<FullNode> res;
                Class entityType = Entity.entityTypeFromSegmentType(st);

                try (Stream<FullNode> next = (Stream<FullNode>) getChildNodes(parent, entityType)) {
                    res = next.collect(Collectors.toList());
                }

                ret = Stream.concat(ret, res.stream());
            }
        }

        return ret;
    }

    /**
     * This had an out-of-place name. Use {@link #getAllChildNodes(RelativePath)} instead.
     *
     * @param parent the parent of which to return the children
     * @return the stream of all children of the parent
     */
    @Deprecated
    default Stream<FullNode> getAllChildrenWithAttachments(RelativePath parent) {
        return getAllChildNodes(parent);
    }

    /**
     * Represents an entity in the inventory structure together with the attachment assigned to it by the inventory
     * structure builder.
     */
    final class FullNode {
        static final FullNode EMPTY = new FullNode(null, null);

        private final Entity.Blueprint entity;
        private final Object attachment;

        public FullNode(Entity.Blueprint entity, Object attachment) {
            this.entity = entity;
            this.attachment = attachment;
        }

        public Entity.Blueprint getEntity() {
            return entity;
        }

        public Object getAttachment() {
            return attachment;
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FullNode)) return false;

            FullNode fullNode = (FullNode) o;

            return entity != null ? entity.equals(fullNode.entity) : fullNode.entity == null;

        }

        @Override public int hashCode() {
            return entity != null ? entity.hashCode() : 0;
        }
    }

    /**
     * This enum lists all the entity types that can be part of a inventory structure. This is a subset of all entity
     * types because not all entities are hashable.
     */
    enum EntityType {
        //the order is significant.. the latter cannot exist without (some of) the prior
        feed(Feed.class, Feed.Blueprint.class, SegmentType.f),
        resourceType(ResourceType.class, ResourceType.Blueprint.class, SegmentType.rt),
        metricType(MetricType.class, MetricType.Blueprint.class, SegmentType.mt),
        operationType(OperationType.class, OperationType.Blueprint.class, SegmentType.ot),
        metric(Metric.class, Metric.Blueprint.class, SegmentType.m),
        resource(Resource.class, Resource.Blueprint.class, r),
        dataEntity(DataEntity.class, DataEntity.Blueprint.class, SegmentType.d);

        public final Class<? extends Entity> elementType;
        public final Class<? extends Entity.Blueprint> blueprintType;
        public final SegmentType segmentType;

        public static EntityType of(Class<?> type) {
            for (EntityType t : EntityType.values()) {
                if (type.equals(t.elementType)) {
                    return t;
                }
            }

            throw new IllegalArgumentException("Unsupported type of entity: " + type);
        }

        public static EntityType of(SegmentType seg) {
            for (EntityType t : EntityType.values()) {
                if (seg == t.segmentType) {
                    return t;
                }
            }

            throw new IllegalArgumentException("Unsupported type of path segment: " + seg);
        }

        public static boolean supports(SegmentType seg) {
            for (EntityType t : EntityType.values()) {
                if (seg == t.segmentType) {
                    return true;
                }
            }

            return false;
        }

        public static EntityType ofBlueprint(Class<?> type) {
            for (EntityType t : EntityType.values()) {
                if (type.equals(t.blueprintType)) {
                    return t;
                }
            }

            return null;
        }

        EntityType(Class<? extends Entity> elementType,
                   Class<? extends Entity.Blueprint> blueprintType, SegmentType segmentType) {
            this.elementType = elementType;
            this.blueprintType = blueprintType;
            this.segmentType = segmentType;
        }
    }

    /**
     * Represents the structure of the inventory off-line, without access to an inventory instance. This implies that
     * an instance holds on to all data required and thus can occupy a lot of memory. On the other hand, it is
     * serializable.
     * <p>
     * This is not directly instantiable but rather can be either copied from another structure (possibly lazily
     * loaded) or built using a {@link Builder}.
     */
    @ApiModel("InventoryStructure")
    class Offline<Root extends Entity.Blueprint> implements InventoryStructure<Root>, Serializable {

        private final Root root;
        private final Map<RelativePath, Map<EntityType, Set<FullNode>>> children;
        private final Map<RelativePath, FullNode> entities;

        private Offline(Root root, Map<RelativePath, FullNode> entities,
                        Map<RelativePath, Map<EntityType, Set<FullNode>>> children) {
            this.root = root;
            this.children = children;
            this.entities = entities;
            if (!entities.containsKey(EmptyRelativePath.I)) {
                entities.put(EmptyRelativePath.I, new FullNode(root, null));
            }
        }

        public static <R extends Entity.Blueprint> Offline<R> copy(InventoryStructure<R> other) {
            return copy(other, false);
        }

        public static <R extends Entity.Blueprint> Offline<R> copy(InventoryStructure<R> other,
                                                                   boolean withAttachments) {
            Map<RelativePath, FullNode> entities = new HashMap<>();

            ElementTypeVisitor<Void, RelativePath.Extender> visitor =
                    new ElementTypeVisitor.Simple<Void, RelativePath.Extender>() {
                        @Override protected Void defaultAction(SegmentType elementType,
                                                               RelativePath.Extender parentPath) {

                            @SuppressWarnings({"unchecked", "rawtypes"})
                            Class<Entity> childType =
                                    (Class) Entity.typeFromSegmentType(elementType);

                            impl(childType, parentPath);

                            return null;
                        }

                        private <E extends Entity>
                        void impl(Class<E> childType, RelativePath.Extender parent) {
                            SegmentType childSeg = Entity.segmentTypeFromType(childType);
                            if (parent.canExtendTo(childSeg)) {
                                RelativePath parentPath = parent.get();

                                FullNode parentNode = entities.get(parentPath);
                                if (parentNode == null) {
                                    if (parentPath.isDefined()) {
                                        throw new IllegalStateException("Could not find the tracked children of a" +
                                                " parent " + parentPath + " during inventory structure copy. This is a " +
                                                "bug.");
                                    } else {
                                        Object att = withAttachments
                                                ? other.getNode(EmptyRelativePath.I).getAttachment()
                                                : null;
                                        parentNode = new FullNode(other.getRoot(), att);
                                        entities.put(parentPath, parentNode);
                                    }
                                }

                                //we cannot recursively call ourselves while evaluating the stream, because that
                                //would result in nested transactions, which are not supported...
                                List<FullNode> otherChildren;
                                try (Stream<FullNode> s = other.getChildNodes(parent.get(), childType)) {
                                    otherChildren = s.collect(Collectors.toList());
                                }

                                otherChildren.forEach(c -> {
                                    RelativePath.Extender childPath = parentPath.modified()
                                            .extend(childSeg, c.getEntity().getId());

                                    RelativePath cp = childPath.get();

                                    FullNode childNode = entities.get(cp);
                                    if (childNode == null) {
                                        Object att = withAttachments
                                                ? other.getNode(cp).getAttachment()
                                                : null;
                                        childNode = new FullNode(c.getEntity(), att);
                                        entities.put(cp, childNode);
                                    }

                                    for (SegmentType childChildSeg : SegmentType.values()) {
                                        if (childPath.canExtendTo(childChildSeg)
                                                && EntityType.supports(childChildSeg)) {
                                            ElementTypeVisitor.accept(childChildSeg, this, childPath);
                                        }
                                    }
                                });
                            }
                        }
                    };

            R root = other.getRoot();

            RelativePath empty = EmptyRelativePath.I;

            //this is important. We need to eagerly collect the children because we don't know if the other structure
            //is online or offline. The backends usually don't support nested transactions and if the other is online
            //inventory structure, then getAllChildren() opens a transaction. If we then processed the items in all
            //children one by one from the stream, the visitor would then call to fetch other children recursively,
            //potentially spawning other transactions. Backends that do not support nested txs would then freak out.
            //The solution therefore is to fetch children eagerly (closing the stream) and then process them 1 by 1.
            List<Entity.Blueprint> acs;
            try (Stream<Entity.Blueprint> s = other.getAllChildren(empty)) {
                acs = s.collect(Collectors.toList());
            }
            acs.forEach(b -> ElementTypeVisitor.accept(Blueprint.getSegmentTypeOf(b), visitor,
                    empty.modified()));

            Map<RelativePath, Map<EntityType, Set<FullNode>>> children = new HashMap<>();
            Map<RelativePath, FullNode> blueprints = new HashMap<>();

            for (Map.Entry<RelativePath, FullNode> e : entities.entrySet()) {
                //handle entities
                RelativePath entityPath = e.getKey();
                FullNode entity = e.getValue();

                blueprints.put(entityPath, entity);

                RelativePath parent = entityPath.up();
                if (parent.equals(entityPath)) {
                    //if we can no longer go up, don't add this entity as a child of its parent. It would add itself
                    //as its own child...
                    continue;
                }
                EntityType entityType = EntityType.of(Blueprint.getEntityTypeOf(entity.getEntity()));

                Map<EntityType, Set<FullNode>> childrenByType = children.computeIfAbsent(parent, k -> new HashMap<>());

                Set<FullNode> childrenBlueprints =
                        childrenByType.computeIfAbsent(entityType, k -> new TreeSet<>(Helper.ENTITY_ORDER));
                childrenBlueprints.add(entity);
            }

            return new Offline<>(root, blueprints, children);
        }


        /**
         * You can use this method if you have an existing inventory structure and want to make modifications to it.
         *
         * @return a builder seeded with this inventory structure
         */
        public InventoryStructure.Builder<Root> asBuilder() {
            RelativePath rootPath = EmptyRelativePath.I;
            Object attachment = getNode(rootPath).getAttachment();
            return new InventoryStructure.Builder<>(root, attachment, EmptyRelativePath.I, entities, children);
        }

        public static <R extends Entity.Blueprint> Builder<R> of(R root) {
            return of(root, null);
        }

        public static <R extends Entity.Blueprint> Builder<R> of (R root, Object attachment) {
            return new Builder<>(root, attachment);
        }

        @Override public Root getRoot() {
            return root;
        }

        @SuppressWarnings("unchecked") @Override
        public <E extends Entity, B extends Entity.Blueprint> Stream<B>
        getChildren(RelativePath parent, Class<E> childType) {
            return (Stream<B>) children.getOrDefault(parent, Collections.emptyMap())
                    .getOrDefault(EntityType.of(childType), Collections.emptySet())
                    .stream().map(FullNode::getEntity);
        }

        @Override public Entity.Blueprint get(RelativePath path) {
            return entities.getOrDefault(path, FullNode.EMPTY).getEntity();
        }

        @Override public FullNode getNode(RelativePath path) {
            return entities.get(path);
        }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Offline<?> offline = (Offline<?>) o;

            if (!root.equals(offline.root)) return false;
            if (!children.equals(offline.children)) return false;
            return entities.equals(offline.entities);

        }

        @Override public int hashCode() {
            int result = root.hashCode();
            result = 31 * result + children.hashCode();
            result = 31 * result + entities.hashCode();
            return result;
        }
    }

    abstract class AbstractBuilder<This extends AbstractBuilder<?>> {
        RelativePath myPath;
        final Map<RelativePath, Map<EntityType, Set<FullNode>>> children;
        final Map<RelativePath, FullNode> blueprints;

        private AbstractBuilder(RelativePath myPath, Map<RelativePath, FullNode> blueprints,
                                Map<RelativePath, Map<EntityType, Set<FullNode>>> children) {
            this.myPath = myPath;
            this.children = children;
            this.blueprints = blueprints;
        }

        /**
         * Equivalent to calling {@code startChilld(child, null)}.
         * @see #startChild(Entity.Blueprint, Object)
         */
        public ChildBuilder<This> startChild(Entity.Blueprint child) {
            return startChild(child, null);
        }

        /**
         * Starts building a new child of the currently built entity.
         *
         * @param child the child entity blueprint
         * @param childAttachment the attachment to store along with the child
         * @return a new child builder
         * @throws IllegalArgumentException if the provided child cannot be contained in the currently built entity
         * (i.e. a resource type cannot be contained in a resource for example).
         */
        public ChildBuilder<This> startChild(Entity.Blueprint child, Object childAttachment) {
            RelativePath.Extender extender = myPath.modified();
            Class<? extends AbstractElement> childType = Blueprint.getEntityTypeOf(child);

            SegmentType childSeg = Blueprint.getSegmentTypeOf(child);

            if (!extender.canExtendTo(childSeg)) {
                throw new IllegalArgumentException("Cannot extend path " + myPath + " with child of type " + childType);
            }

            RelativePath childPath = extender.extend(childSeg, child.getId()).get();

            Set<FullNode> bls = getChildrenOfType(EntityType.of(childType));

            FullNode node = new FullNode(child, childAttachment);

            bls.add(node);

            blueprints.put(childPath, node);

            return new ChildBuilder<>(castThis(), childPath, blueprints, children);
        }

        public RelativePath getPath() {
            return myPath;
        }

        public Entity.Blueprint getBlueprint() {
            return blueprints.get(myPath).getEntity();
        }

        public FullNode getNode() {
            return blueprints.get(myPath);
        }

        /**
         * Returns a child builder of a pre-existing child.
         * @param childPath the path to the child
         * @return the child builder or null
         */
        public ChildBuilder<This> getChild(Path.Segment childPath) {
            Map<EntityType, Set<FullNode>> myChildren = children.get(myPath);
            if (myChildren == null) {
                return null;
            }

            EntityType childType = EntityType.of(childPath.getElementType());
            Set<FullNode> childrenOfType = myChildren.get(childType);

            return childrenOfType.stream().filter(child -> child.getEntity().getId().equals(childPath.getElementId()))
                    .findAny().map(child -> {
                        RelativePath rp = myPath.modified().extend(childPath).get();
                        return new ChildBuilder<>(castThis(), rp, blueprints, children);
                    }).orElse(null);
        }

        public This removeChild(Path.Segment childPath) {
            ChildBuilder<This> childBuilder = getChild(childPath);

            if (childBuilder != null) {
                childBuilder.remove();
            }

            return castThis();
        }

        public Set<Path.Segment> getChildrenPaths() {
            Map<EntityType, Set<FullNode>> myChildren = children.get(myPath);
            if (myChildren == null) {
                return Collections.emptySet();
            }

            return myChildren.values().stream().flatMap(Collection::stream)
                    .map(b -> new Path.Segment(Blueprint.getSegmentTypeOf(b.getEntity()), b.getEntity().getId()))
                    .collect(Collectors.toSet());
        }

        public This removeAllChildren() {
            getChildrenPaths().forEach(this::removeChild);
            children.remove(myPath);
            return castThis();
        }

        public This replace(Entity.Blueprint blueprint) {
            return replace(blueprint, null);
        }

        /**
         * Replaces the blueprint on this position in the structure with another. The blueprint must have the same type
         * as the original one.
         *
         * @param blueprint the blueprint to replace the current with
         * @param attachment the object to attach to the entity blueprint
         * @return this builder
         */
        public This replace(Entity.Blueprint blueprint, Object attachment) {
            removeAllChildren();

            FullNode myBl = blueprints.get(myPath);
            if (!myBl.getEntity().getClass().equals(blueprint.getClass())) {
                throw new IllegalArgumentException("Blueprint " + blueprint + " not of the same type as "
                        + myBl.getEntity());
            }

            doReplace(blueprint, attachment);

            return castThis();
        }

        abstract void doReplace(Entity.Blueprint blueprint, Object attachment);

        Set<FullNode> getChildrenOfType(EntityType childType) {
            Map<EntityType, Set<FullNode>> cs = children.computeIfAbsent(myPath, k -> new EnumMap<>(EntityType.class));
            return cs.computeIfAbsent(childType, k -> new TreeSet<>(ENTITY_ORDER));
        }

        /**
         * Adds a new child to this entity without the possibility to add further grand-children to the child.
         *
         * @param child the child to add to this entity
         * @return this builder
         * @throws IllegalArgumentException if the provided child cannot be contained in the currently built entity
         * (i.e. a resource type cannot be contained in a resource for example).
         */
        public This addChild(Entity.Blueprint child) {
            return addChild(child, null);
        }

        public This addChild(Entity.Blueprint child, Object attachment) {
            startChild(child, attachment).end();
            return castThis();
        }

        @SuppressWarnings("unchecked")
        This castThis() {
            return (This) this;
        }
    }

    final class Builder<Root extends Entity.Blueprint> extends AbstractBuilder<Builder<Root>> {
        private final Root root;

        private Builder(Root root, Object attachment, RelativePath myPath,
                       Map<RelativePath, FullNode> blueprints,
                       Map<RelativePath, Map<EntityType, Set<FullNode>>> children) {
            super(myPath, blueprints, children);
            this.root = root;
            this.blueprints.put(EmptyRelativePath.I, new FullNode(root, attachment));
        }

        public Builder(Root root) {
            this(root, null);
        }

        public Builder(Root root, Object attachment) {
            this(root, attachment, EmptyRelativePath.I, new HashMap<>(), new HashMap<>());
        }

        public Offline<Root> build() {
            return new Offline<>(root, blueprints, children);
        }

        @Override void doReplace(Entity.Blueprint blueprint, Object attachment) {
            blueprints.put(myPath, new FullNode(blueprint, attachment));
            children.remove(myPath);
        }
    }

    final class ChildBuilder<ParentBuilder extends AbstractBuilder<?>> extends
            AbstractBuilder<ChildBuilder<ParentBuilder>> {
        final ParentBuilder parentBuilder;

        private ChildBuilder(ParentBuilder parentBuilder, RelativePath parent,
                             Map<RelativePath, FullNode> blueprints,
                             Map<RelativePath, Map<EntityType, Set<FullNode>>> children) {
            super(parent, blueprints, children);
            this.parentBuilder = parentBuilder;
        }

        /**
         * Ends the current child and returns the builder of the parent entity.
         * @return the builder of the parent entity
         */
        public ParentBuilder end() {
            return parentBuilder;
        }

        /**
         * Removes this child from the structure.
         * @return the parent builder
         */
        public ParentBuilder remove() {
            removeAllChildren();
            Set<FullNode> siblings = getSiblings();
            FullNode myBlueprint = blueprints.remove(myPath);
            siblings.remove(myBlueprint);
            return parentBuilder;
        }

        @Override void doReplace(Entity.Blueprint blueprint, Object attachment) {
            Set<FullNode> siblings = getSiblings();
            FullNode myBlueprint = blueprints.remove(myPath);
            siblings.remove(myBlueprint);

            FullNode myNode = new FullNode(blueprint, attachment);

            siblings.add(myNode);
            children.remove(myPath);
            myPath = parentBuilder.myPath.modified().extend(Blueprint.getSegmentTypeOf(blueprint), blueprint.getId())
                    .get();
            blueprints.put(myPath, myNode);
        }

        private Set<FullNode> getSiblings() {
            FullNode myBlueprint = blueprints.get(myPath);
            Map<EntityType, Set<FullNode>> siblingsByType = children.get(parentBuilder.myPath);
            EntityType myType = EntityType.of(Blueprint.getEntityTypeOf(myBlueprint.getEntity()));
            return siblingsByType.get(myType);
        }
    }
}

class EmptyRelativePath {
    static final RelativePath I = RelativePath.empty().get();
}

class Helper {
    static final Comparator<InventoryStructure.FullNode> ENTITY_ORDER = (a, b) -> {
        InventoryStructure.EntityType aType = InventoryStructure.EntityType.ofBlueprint(a.getEntity().getClass());
        InventoryStructure.EntityType bType = InventoryStructure.EntityType.ofBlueprint(b.getEntity().getClass());

        int ret = aType.ordinal() - bType.ordinal();
        if (ret == 0) {
            ret = a.getEntity().getId().compareTo(b.getEntity().getId());
        }

        return ret;
    };
}
