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

/**
 * This is a wrapper class to hold various interfaces defining available functionality on relationships.
 *
 * @author Lukas Krejci
 * @since 0.0.1
 */
public final class Relationships {
    private Relationships() {

    }

    /**
     * The list of possible relationship (aka edges) direction. Relationships are not bidirectional.
     */
    public enum Direction {
        /**
         * Relative to the current position in the inventory traversal, this value expresses such relationships
         * that has me (the entity(ies) on the current pos) as a source(s).
         */
        outgoing,

        /**
         * Relative to the current position in the inventory traversal, this value expresses such relationships
         * that has me (the entity(ies) on the current pos) as a target(s).
         */
        incoming,

        /**
         * Relative to the current position in the inventory traversal, this value expresses all the relationships
         * I (the entity(ies) on the current pos) have with other entity(ies).
         */
        both
    }
}
