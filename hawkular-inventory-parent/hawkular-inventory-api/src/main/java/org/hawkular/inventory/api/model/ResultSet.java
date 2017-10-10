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

import static com.fasterxml.jackson.annotation.JsonInclude.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
@JsonDeserialize(using = JacksonDeserializer.ResultSetDeserializer.class)
public class ResultSet<T> {

    @JsonInclude(Include.NON_NULL)
    private List<T> results;

    @JsonInclude(Include.NON_NULL)
    private Long resultSize;

    @JsonInclude(Include.NON_NULL)
    private Long startOffset;

    public ResultSet(@JsonProperty("results") List<T> results,
                     @JsonProperty("resultSize") Long resultSize,
                     @JsonProperty("startOffset") Long startOffset) {
        this.results = results;
        this.resultSize = resultSize;
        this.startOffset = startOffset;
    }

    public Collection<T> getResults() {
        return Collections.unmodifiableList(results);
    }

    public Long getResultSize() {
        return resultSize;
    }

    public Long getStartOffset() {
        return startOffset;
    }
}
