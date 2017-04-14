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

/**
 * A visitor interface to accept different kinds of entities available in Hawkular.
 *
 * @author Lukas Krejci
 * @since 0.0.1
 */
public interface ElementVisitor<R, P> {

    R visitTenant(Tenant tenant, P parameter);

    R visitEnvironment(Environment environment, P parameter);

    R visitFeed(Feed feed, P parameter);

    R visitMetric(Metric metric, P parameter);

    R visitMetricType(MetricType definition, P parameter);

    R visitResource(Resource resource, P parameter);

    R visitResourceType(ResourceType type, P parameter);

    R visitRelationship(Relationship relationship, P parameter);

    R visitData(DataEntity data, P parameter);

    R visitOperationType(OperationType operationType, P parameter);

    R visitMetadataPack(MetadataPack metadataPack, P parameter);
}
