/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates
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
package org.hawkular.bus.restclient;

import org.apache.http.HttpResponse;

/**
 * Thrown when the {@link RestClient} hits an error condition.
 */
public class RestClientException extends Exception {
    private static final long serialVersionUID = 1L;

    private final HttpResponse httpResponse;

    /**
     * If the exception occurred after a response was received, and that response is available
     * this will be that response.
     *
     * @return the response that can help determine the error that occurred
     */
    public HttpResponse getHttpResponse() {
        return httpResponse;
    }

    public RestClientException() {
        this((HttpResponse) null);
    }

    public RestClientException(String message, Throwable cause) {
        this((HttpResponse) null, message, cause);
    }

    public RestClientException(String message) {
        this((HttpResponse) null, message);
    }

    public RestClientException(Throwable cause) {
        this((HttpResponse) null, cause);
    }

    public RestClientException(HttpResponse httpResponse) {
        super();
        this.httpResponse = httpResponse;
    }

    public RestClientException(HttpResponse httpResponse, String message, Throwable cause) {
        super(message, cause);
        this.httpResponse = httpResponse;
    }

    public RestClientException(HttpResponse httpResponse, String message) {
        super(message);
        this.httpResponse = httpResponse;
    }

    public RestClientException(HttpResponse httpResponse, Throwable cause) {
        super(cause);
        this.httpResponse = httpResponse;
    }
}
