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
package org.hawkular.handlers;

import io.vertx.ext.web.Router;

/**
 * Represent a Vert.X/Netty Handler deployed on HawkularServer.
 * It is responsible to add Rest logic under a Vert.X Router.
 * Rest handlers need to be annotated with {@see RestEndpoint}.
 *
 * All handlers need to be grouped in a package and need a {@see BaseApplication} to define a base url.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public interface RestHandler {

    /**
     * Invoked by HawkularServer on handler registration.
     * It is responsible to attach handlers logic into a Vert.X Router.
     *
     * @param baseUrl for this handler, defined by the package {@see BaseApplication}
     * @param router Vert.X Router provided by HawkularServer
     */
    void initRoutes(String baseUrl, Router router);
}
