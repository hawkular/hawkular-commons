/*
 * Copyright 2014-2016 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.jaxrs.filter.tenant;

import java.lang.reflect.Method;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;

/**
 * A JAX-RS dynamic feature that adds the {@link TenantFilter} to all methods which have {@link TenantRequired}
 * effectively as "true", which might be the default value for {@link TenantRequired}. In the usual case, this feature
 * is added to all classes and methods, except when classes/methods are annotated with "false".
 *
 * @author Juraci Paixão Kröhling
 */
@Provider
public class TenantFeature implements DynamicFeature {
    private static final TenantFilter TENANT_FILTER = new TenantFilter();

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        Class<?> resourceClass = resourceInfo.getResourceClass();
        Method method = resourceInfo.getResourceMethod();

        boolean required = true;
        if (resourceClass.isAnnotationPresent(TenantRequired.class)) {
            required = resourceClass.getAnnotation(TenantRequired.class).value();
        }

        if (method.isAnnotationPresent(TenantRequired.class)) {
            required = method.getAnnotation(TenantRequired.class).value();
        }

        if (required) {
            context.register(TENANT_FILTER);
        }
    }
}
