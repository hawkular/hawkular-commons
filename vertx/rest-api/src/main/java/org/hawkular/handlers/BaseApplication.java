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

/**
 * Represent a Rest based application deployed on a HawkularServer.
 * There must be one defined at same package where {@see RestHandler} handlers are defined.
 * It is responsible of initialization and pre-shutdown tasks as well as provide the baseUrl to package handlers.
 *
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public interface BaseApplication {

    /**
     * Invoked by HawkularServer before to load package {@see RestHandler}
     */
    void start();

    /**
     * Invoked by HawkularServer before to stop the server
     */
    void stop();

    /**
     * @return base url used by package handlers
     */
    String baseUrl();
}
