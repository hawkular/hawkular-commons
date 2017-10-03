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
 * @author Joel Takvorian
 */
public class ResourceFilter {
    private boolean rootOnly;
    private String typeId;
    private String feedId;

    public ResourceFilter(boolean rootOnly, String feedId, String typeId) {
        this.rootOnly = rootOnly;
        this.feedId = feedId;
        this.typeId = typeId;
    }

    public static ResourceFilter rootOnly() {
        return new ResourceFilter(true, null, null);
    }

    public static ResourceFilter ofType(String typeId) {
        return new ResourceFilter(false, null, typeId);
    }

    public static ResourceFilter forFeed(String feedId) {
        return new ResourceFilter(false, feedId, null);
    }

    public ResourceFilter andFeed(String feedId) {
        this.feedId = feedId;
        return this;
    }

    public ResourceFilter andType(String typeId) {
        this.typeId = typeId;
        return this;
    }

    public ResourceFilter andRootOnly() {
        this.rootOnly = true;
        return this;
    }

    public boolean isRootOnly() {
        return rootOnly;
    }

    public String getTypeId() {
        return typeId;
    }

    public String getFeedId() {
        return feedId;
    }
}
