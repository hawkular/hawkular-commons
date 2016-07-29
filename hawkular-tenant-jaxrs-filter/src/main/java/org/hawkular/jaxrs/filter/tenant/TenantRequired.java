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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Explicitly marks a class or method on the requirement for the Hawkular-Tenant HTTP header. Method annotations have
 * priority over class annotations, so, if a method sets this as "true" and the class sets as "false", a Hawkular-Tenant
 * will be required for calls that end up on that method.
 *
 * @author Juraci Paixão Kröhling
 */
@Retention(RUNTIME)
@Target({METHOD, TYPE})
public @interface TenantRequired {
    boolean value() default true;
}
