/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
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
 * This is the most generic interface for positions in the graph traversal from which one can resolve a single entity
 * using its id.
 *
 * @param <Single> the access interface to the entity
 * @param <Address> the type of address to use to find the entity
 * @author Lukas Krejci
 * @since 0.0.1
 */
public interface ResolvingToSingle<Single, Address> {

    /**
     * Tries to find a single entity in the current position in the inventory traversal.
     *
     * @param address the identification of sorts of the entity to find in the current traversal position
     * @return access interface to the entity
     */
    Single get(Address address) throws EntityNotFoundException;
}
