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
import java.util.Map;
import java.util.Set;

import org.hawkular.inventory.paths.CanonicalPath;
import org.hawkular.inventory.paths.SegmentType;

import io.swagger.annotations.ApiModel;

/**
 * Metric describes a single metric that is sent out from a feed. Each metric has a unique ID and a type. Metrics live
 * in an environment and can be "incorporated" by {@link Resource resources} (surprisingly, many resources can
 * incorporate a single metric).
 *
 * @author Lukas Krejci
 * @since 0.0.1
 */
@ApiModel(description = "A metric represents a monitored \"quality\". Its metric type specifies the unit in which" +
        " the metric reports its values and the collection interval specifies how often the feed should be collecting" +
        " the metric for changes in value.",
        parent = Entity.class)
public final class Metric extends Entity {

    public static final SegmentType SEGMENT_TYPE = SegmentType.m;

    private final MetricType type;
    private final Long collectionInterval;

    /**
     * Jackson support
     */
    @SuppressWarnings("unused")
    private Metric() {
        type = null;
        collectionInterval = null;
    }

    public Metric(CanonicalPath path, MetricType type) {
        this(null, path, type, null, null);
    }

    public Metric(String name, CanonicalPath path, MetricType type) {
        this(name, path, type, null ,null);
    }

    public Metric(CanonicalPath path, MetricType type, Long collectionInterval) {
        this(null, path, type, collectionInterval, null);
    }

    public Metric(CanonicalPath path, MetricType type, Map<String, Object> properties) {
        this(null, path, type, null, properties);
    }

    public Metric(String name, CanonicalPath path, MetricType type, Long collectionInterval, Map<String, Object> properties) {
        super(name, path, properties);
        this.type = type;
        this.collectionInterval = collectionInterval;
    }

    public MetricType getType() {
        return type;
    }

    public Long getCollectionInterval() {
        return collectionInterval;
    }

    @Override
    protected void appendToString(StringBuilder toStringBuilder) {
        super.appendToString(toStringBuilder);
        toStringBuilder.append(", definition=").append(type);
    }

    @Override
    public <R, P> R accept(ElementVisitor<R, P> visitor, P parameter) {
        return visitor.visitMetric(this, parameter);
    }

    /**
     * Data required to create a new metric.
     *
     * <p>Note that tenantId, etc., are not needed here because they are provided by the context in which the
     * {@link org.hawkular.inventory.api.WriteInterface#create(org.hawkular.inventory.api.model.Blueprint)} method is
     * called.
     */
    @ApiModel("MetricBlueprint")
    public static final class Blueprint extends Entity.Blueprint {
        private final String metricTypePath;
        private final Long collectionInterval;

        public static Builder builder() {
            return new Builder();
        }

        /**
         * Jackson support
         */
        @SuppressWarnings("unused")
        private Blueprint() {
            metricTypePath = null;
            collectionInterval = null;
        }

        public Blueprint(String metricTypePath, String id) {
            this(metricTypePath, id, Collections.emptyMap());
        }

        public Blueprint(String metricTypePath, String id, Map<String, Object> properties) {
            super(id, properties);
            this.metricTypePath = metricTypePath;
            this.collectionInterval = null;
        }

        public Blueprint(String metricTypePath, String id, Map<String, Object> properties,
                         Map<String, Set<CanonicalPath>> outgoing,
                         Map<String, Set<CanonicalPath>> incoming) {
            super(id, properties, outgoing, incoming);
            this.metricTypePath = metricTypePath;
            this.collectionInterval = null;
        }

        public Blueprint(String metricTypePath, String id, String name, Map<String, Object> properties,
                         Map<String, Set<CanonicalPath>> outgoing,
                         Map<String, Set<CanonicalPath>> incoming) {
            super(id, name, properties, outgoing, incoming);
            this.metricTypePath = metricTypePath;
            this.collectionInterval = null;
        }

        public Blueprint(String metricTypePath, String id, String name, Long collectionInterval,
                         Map<String, Object> properties,
                         Map<String, Set<CanonicalPath>> outgoing,
                         Map<String, Set<CanonicalPath>> incoming) {
            super(id, name, properties, outgoing, incoming);
            this.metricTypePath = metricTypePath;
            this.collectionInterval = collectionInterval;
        }

        public String getMetricTypePath() {
            return metricTypePath;
        }

        public Long getCollectionInterval() {
            return collectionInterval;
        }

        @Override
        public <R, P> R accept(ElementBlueprintVisitor<R, P> visitor, P parameter) {
            return visitor.visitMetric(this, parameter);
        }

        public static final class Builder extends Entity.Blueprint.Builder<Blueprint, Builder> {
            private String metricTypeId;
            private Long collectionInterval;

            public Builder withMetricTypePath(String metricTypePath) {
                this.metricTypeId = metricTypePath;
                return this;
            }

            public Builder withInterval(Long interval) {
                this.collectionInterval = interval;
                return this;
            }

            @Override
            public Blueprint build() {
                return new Blueprint(metricTypeId, id, name, collectionInterval, properties, outgoing, incoming);
            }
        }
    }
}
