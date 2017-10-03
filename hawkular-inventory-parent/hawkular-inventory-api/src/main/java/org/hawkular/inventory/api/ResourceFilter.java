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
    private final boolean rootOnly;
    private final String typeId;
    private final String feedId;

    public ResourceFilter(boolean rootOnly, String feedId, String typeId) {
        this.rootOnly = rootOnly;
        this.feedId = feedId;
        this.typeId = typeId;
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

    public static ResourceFilter.Builder builder() {
        return new ResourceFilter.Builder(false, null, null);
    }

    public static ResourceFilter.Builder rootOnly() {
        return new ResourceFilter.Builder(true, null, null);
    }

    public static ResourceFilter.Builder ofType(String typeId) {
        return new ResourceFilter.Builder(false, null, typeId);
    }

    public static ResourceFilter.Builder forFeed(String feedId) {
        return new ResourceFilter.Builder(false, feedId, null);
    }

    public static class Builder {

        private boolean rootOnly;
        private String typeId;
        private String feedId;

        private Builder(boolean rootOnly, String feedId, String typeId) {
            this.rootOnly = rootOnly;
            this.feedId = feedId;
            this.typeId = typeId;
        }

        public Builder andFeed(String feedId) {
            this.feedId = feedId;
            return this;
        }

        public Builder andType(String typeId) {
            this.typeId = typeId;
            return this;
        }

        public Builder andRootOnly() {
            this.rootOnly = true;
            return this;
        }

        public ResourceFilter build() {
            return new ResourceFilter(rootOnly, feedId, typeId);
        }
    }
}
