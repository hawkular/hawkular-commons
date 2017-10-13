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
package org.hawkular.inventory.handlers;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ResourceMethodRegistry;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Discovering REST resources path based on resteasy {@link ResourceMethodRegistry}
 * @author Joel Takvorian
 */
public final class ResourcesDiscovery {

    private ResourcesDiscovery() {
    }

    public static List<Resource> discover(ResourceMethodRegistry registry) {
        return registry.getBounded().entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .map(entry -> {
                    String path = entry.getKey();
                    List<ResourceMethod> methods = entry.getValue().stream()
                            .filter(resourceInvoker -> resourceInvoker instanceof ResourceMethodInvoker)
                            .map(resourceInvoker -> {
                                ResourceMethodInvoker rmi = (ResourceMethodInvoker) resourceInvoker;
                                String producing = null;
                                if (rmi.getProduces() != null && rmi.getProduces().length > 0) {
                                    producing = rmi.getProduces()[0].toString();
                                }
                                String consuming = null;
                                if (rmi.getConsumes() != null && rmi.getConsumes().length > 0) {
                                    consuming = rmi.getConsumes()[0].toString();
                                }
                                return new ResourceMethod(rmi.getHttpMethods().iterator().next(), producing, consuming);
                            }).collect(Collectors.toList());
                    return new Resource(path, methods);
                }).collect(Collectors.toList());
    }

    public static class Resource {
        private final String path;
        private final List<ResourceMethod> methods;

        private Resource(String path,
                         List<ResourceMethod> methods) {
            this.path = path;
            this.methods = methods;
        }

        public String getPath() {
            return path;
        }

        public List<ResourceMethod> getMethods() {
            return methods;
        }
    }
    public static class ResourceMethod {
        private final String verb;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private final String producing;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private final String consuming;

        private ResourceMethod(String verb, String producing, String consuming) {
            this.verb = verb;
            this.producing = producing;
            this.consuming = consuming;
        }

        public String getVerb() {
            return verb;
        }

        public String getProducing() {
            return producing;
        }

        public String getConsuming() {
            return consuming;
        }
    }
}
