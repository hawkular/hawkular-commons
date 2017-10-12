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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * @author Jay Shaughnessy
 * @author Lucas Ponce
 */
public class JacksonDeserializer {

    public static class ResultSetDeserializer extends JsonDeserializer<ResultSet> {
        @Override
        public ResultSet deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {

            ObjectCodec objectCodec = jp.getCodec();
            JsonNode node = objectCodec.readTree(jp);
            long resultSize = node.get("resultSize").asLong();
            long startOffset = node.get("startOffset").asLong();
            List results = new ArrayList<>();
            if (node.get("results").isArray() && node.get("results").size() > 0) {
                JsonNode first = node.get("results").get(0);
                boolean resourceNode = first.get("children") != null;
                boolean resourceWithType = first.get("type") != null;
                Iterator<JsonNode> elements = node.get("results").elements();
                while (elements.hasNext()) {
                    JsonNode element = elements.next();
                    if (resourceNode) {
                        results.add(objectCodec.treeToValue(element, ResourceNode.class));
                    } else if (resourceWithType) {
                        results.add(objectCodec.treeToValue(element, Resource.class));
                    } else {
                        results.add(objectCodec.treeToValue(element, ResourceType.class));
                    }
                }
            }
            return new ResultSet(results, resultSize, startOffset);
        }
    }
}
