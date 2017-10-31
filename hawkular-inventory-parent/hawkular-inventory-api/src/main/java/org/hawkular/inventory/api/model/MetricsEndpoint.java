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
package org.hawkular.inventory.api.model;

import java.io.Serializable;

import org.hawkular.commons.doc.DocModel;
import org.hawkular.commons.doc.DocModelProperty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This represents an agent's metrics endpoint that can be scraped for metrics data.
 *
 * @author John Mazzitelli
 */
@DocModel(description = "Representation of an endpoint that can be scraped for metrics data.")
public class MetricsEndpoint implements Serializable {

    public static class Builder {
        private String feedId;
        private String host;
        private Integer port;

        public MetricsEndpoint build() {
            return new MetricsEndpoint(feedId, host, port);
        }

        public Builder feedId(String feedId) {
            this.feedId = feedId;
            return this;
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(Integer port) {
            this.port = port;
            return this;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @DocModelProperty(description = "The feed ID of the metrics endpoint agent.",
            position = 0,
            required = true)
    @JsonInclude(Include.NON_NULL)
    private final String feedId;

    @DocModelProperty(description = "Host where the metrics endpoint is located.",
            position = 1,
            required = true)
    @JsonInclude(Include.NON_NULL)
    private final String host;

    @DocModelProperty(description = "Port on the host where the endpoint is listening.",
            position = 2,
            required = true)
    @JsonInclude(Include.NON_NULL)
    private final Integer port;

    public MetricsEndpoint(@JsonProperty("feedId") String feedId,
            @JsonProperty("host") String host,
            @JsonProperty("port") Integer port) {
        this.feedId = feedId;
        this.host = host;
        this.port = port;
    }

    public String getFeedId() {
        return feedId;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "MetricsEndpoint{" +
                "feedId='" + feedId + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
