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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.logging.Logger;

/**
 * You can use this to consume and produce messages on the Hawkular bus.
 *
 * This class is purposefully designed to be simple with very few dependencies (just
 * requires Apache's httpclient and JBoss Logging). Specifically, you do not need
 * any other Hawkular libraries to be able to use this to send messages to the
 * Hawkular bus.
 */
public class RestClient {
    private static final String DEFAULT_URL_PATH = "/hawkular-bus/message/";

    private static final Logger LOG = Logger.getLogger(RestClient.class);

    private final URL endpoint;
    private final DefaultHttpClient httpclient;

    public static enum Type {
        QUEUE, TOPIC
    }

    /**
     * Creates the sender with a default HTTP endpoint of the REST server.
     *
     * @param host the remote server
     * @param port if null or -1, no port will be specified in the URL
     * @throws RestClientException if the endpoint cannot be used to refer to queues or topics
     *                             or if one of the parameters consists of invalid URL syntax
     */
    public RestClient(String host, Integer port) throws RestClientException {
        this("http", host, port);
    }

    /**
     * Creates the sender with a default endpoint of the REST server.
     *
     * @param protocol http or https
     * @param host the remote server
     * @param port if null or -1, no port will be specified in the URL
     * @throws RestClientException if the endpoint cannot be used to refer to queues or topics
     *                             or if one of the parameters consists of invalid URL syntax
     */
    public RestClient(String protocol, String host, Integer port) throws RestClientException {
        this(buildDefaultURL(protocol, host, port));
    }

    private static URL buildDefaultURL(String protocol, String host, Integer port) throws RestClientException {
        try {
            return new URL(protocol, host, ((port != null) ? port.intValue() : -1), DEFAULT_URL_PATH);
        } catch (MalformedURLException e) {
            throw new RestClientException(e);
        }
    }

    /**
     * Endpoint of the REST server, not including the fragment that consists of the queue/topic name.
     * For example: http://host:8080/hawkular-bus/message/
     *
     * If the given URL does not end with a "/", one will be added to it.
     *
     * @param endpoint the REST server endpoint, not including the queue/topic name
     * @throws RestClientException if the endpoint cannot be used to refer to queues or topics
     */
    public RestClient(URL endpoint) throws RestClientException {
        // make sure the endpoint ends with a "/" since we later will append the queue/topic name to it
        if (endpoint.toString().endsWith("/")){
            this.endpoint = endpoint;
        } else {
            this.endpoint = appendToURL(endpoint, "/");
        }

        // Make sure this endpoint URL is a valid bus endpoint - see if it can be converted to topic or queue URLs.
        // These will throw exceptions if the endpoint URL cannot be used for queues or topics.
        getEndpointForType(Type.QUEUE, "Test");
        getEndpointForType(Type.TOPIC, "Test");

        this.httpclient = new DefaultHttpClient();

        LOG.debugf("Created Hawkular Bus REST client for endpoint [%s]", this.endpoint);
    }

    /**
     * @return the REST endpoint not inluding the fragment that consists of the queue/topic name.
     */
    public URL getEndpoint() {
        return this.endpoint;
    }

    /**
     * Sends a message to the REST endpoint in order to put a message on the given topic.
     *
     * @param topicName name of the topic
     * @param jsonPayload the actual message (as a JSON string) to put on the bus
     * @param headers any headers to send with the message (can be null or empty)
     * @return the response
     * @throws RestClientException if the response was not a 200 status code
     */
    public HttpResponse postTopicMessage(String topicName, String jsonPayload, Map<String, String> headers)
            throws RestClientException {
        return postMessage(Type.TOPIC, topicName, jsonPayload, headers);
    }

    /**
     * Sends a message to the REST endpoint in order to put a message on the given queue.
     *
     * @param queueName name of the queue
     * @param jsonPayload the actual message (as a JSON string) to put on the bus
     * @param headers any headers to send with the message (can be null or empty)
     * @return the response
     * @throws RestClientException if the response was not a 200 status code
     */
    public HttpResponse postQueueMessage(String queueName, String jsonPayload, Map<String, String> headers)
            throws RestClientException {
        return postMessage(Type.QUEUE, queueName, jsonPayload, headers);
    }

    protected HttpResponse postMessage(Type type, String name, String jsonPayload, Map<String, String> headers)
            throws RestClientException {
        URL messageUrl = getEndpointForType(type, name);
        URI uri;
        try {
            uri = messageUrl.toURI();
        } catch (URISyntaxException e) {
            throw new RestClientException(e);
        }
        HttpResponse response = sendPost(uri, jsonPayload, headers);
        return response;
    }

    protected HttpResponse sendPost(URI uri, String jsonPayload, Map<String, String> headers)
            throws RestClientException {
        LOG.tracef("Posting message to bus. uri=[%s], json=[%s], headers[%s]", uri, jsonPayload, headers);

        HttpPost request = null;
        HttpResponse httpResponse = null;

        try {
            request = new HttpPost(uri);
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    request.setHeader(entry.getKey(), entry.getValue());
                }
            }
            request.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));
            httpResponse = httpclient.execute(request);
            StatusLine statusLine = httpResponse.getStatusLine();

            if (statusLine.getStatusCode() != 200) {
                throw new Exception("HTTP post request failed. status-code=[" + statusLine.getStatusCode()
                        + "], reason=["
                        + statusLine.getReasonPhrase() + "], url=[" + request.getURI() + "]");
            }

            return httpResponse;
        } catch (Exception e) {
            String errStr = String.format("Failed to post message to bus via URI [%s]", uri.toString());
            LOG.debugf("%s. Cause=[%s]", errStr, e.toString());
            throw new RestClientException(httpResponse, errStr, e);
        } finally {
            if (request != null) {
                request.releaseConnection();
            }
        }
    }

    protected URL getEndpointForType(Type type, String name) throws RestClientException {
        switch (type) {
            case QUEUE:
                return appendToURL(endpoint, name + "?type=queue");
            case TOPIC:
                return appendToURL(endpoint, name + "?type=topic");
            default: {
                throw new RuntimeException("Invalid type [" + type + "] - please report this bug");
            }
        }
    }

    protected URL appendToURL(URL url, String appendage) throws RestClientException {
        String oldUrlString = url.toString();
        String newUrlString = oldUrlString + appendage;
        try {
            return new URL(newUrlString);
        } catch (MalformedURLException e) {
            throw new RestClientException(String.format("URL [%s] cannot be appended with [%s]", url, appendage), e);
        }
    }
}
