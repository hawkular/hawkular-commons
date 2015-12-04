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
package org.hawkular.bus.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BasicMessageObjectMapperTest {

    @Test
    public void testWithGetterSetterSupport() {
        SomeMessage.FAIL_ON_UNKNOWN_PROPERTIES = true;
        SomeMessage.SUPPORT_GETTER_SETTER = true;

        AnotherMessage msg = new AnotherMessage("1", "2");
        msg.setSomeAttrib("someValue");
        msg.setAnotherAttrib("anotherValue");
        assertNotNull(msg.getOne());
        assertNotNull(msg.getTwo());
        assertNotNull(msg.getSomeAttrib());
        assertNotNull(msg.getAnotherAttrib());

        String json = msg.toJSON();
        System.out.println(json);
        assertNotNull("missing JSON", json);

        AnotherMessage msg2 = AnotherMessage.fromJSON(json, AnotherMessage.class);
        assertNotNull("JSON conversion failed", msg2);
        assertNotSame(msg, msg2);
        assertNotNull(msg2.getOne());
        assertNotNull(msg2.getTwo());
        assertNotNull(msg2.getSomeAttrib());
        assertNotNull(msg2.getAnotherAttrib());
        assertEquals(msg.getOne(), msg2.getOne());
        assertEquals(msg.getTwo(), msg2.getTwo());
        assertEquals(msg.getSomeAttrib(), msg2.getSomeAttrib());
        assertEquals(msg.getAnotherAttrib(), msg2.getAnotherAttrib());
    }

    @Test
    public void testWithoutGetterSetterSupport() {
        SomeMessage.FAIL_ON_UNKNOWN_PROPERTIES = true;
        SomeMessage.SUPPORT_GETTER_SETTER = false;

        AnotherMessage msg = new AnotherMessage("1", "2");
        msg.setSomeAttrib("someValueNoSupport");
        msg.setAnotherAttrib("anotherValueNoSupport");
        assertNotNull(msg.getOne());
        assertNotNull(msg.getTwo());
        assertNotNull(msg.getSomeAttrib());
        assertNotNull(msg.getAnotherAttrib());

        String json = msg.toJSON();
        System.out.println(json);
        assertNotNull("missing JSON", json);

        AnotherMessage msg2 = AnotherMessage.fromJSON(json, AnotherMessage.class);
        assertNotNull("JSON conversion failed", msg2);
        assertNotSame(msg, msg2);
        assertNotNull(msg2.getOne());
        assertNotNull(msg2.getTwo());
        assertNull("Should not have been deserialized, getter/setter support was off", msg2.getSomeAttrib());
        assertNull("Should not have been deserialized, getter/setter support was off", msg2.getAnotherAttrib());
        assertEquals(msg.getOne(), msg2.getOne());
        assertEquals(msg.getTwo(), msg2.getTwo());
        assertNotEquals(msg.getSomeAttrib(), msg2.getSomeAttrib());
        assertNotEquals(msg.getAnotherAttrib(), msg2.getAnotherAttrib());
    }

    @Test
    public void testOverrideStaticDeserializingMapper() {
        // This tests that AbstractMessage is able to get the subclass' overriding ObjectMapper for deserialization
        // which is obtained by invoking a static method on the subclass.
        SomeMessage.SUPPORT_GETTER_SETTER = false;
        String jsonWithAllKnownProperties = "{\"one\":\"1\",\"two\":\"2\"}";
        String jsonWithUnknownProperties = "{\"one\":\"1\",\"two\":\"2\", \"wot\":\"gorilla\"}";

        AnotherMessage msg;

        // because we will not fail on unknown properties, no failures should occur
        SomeMessage.FAIL_ON_UNKNOWN_PROPERTIES = false;
        msg = AnotherMessage.fromJSON(jsonWithAllKnownProperties, AnotherMessage.class);
        assertEquals("1", msg.getOne());
        assertEquals("2", msg.getTwo());
        assertNull(msg.getSomeAttrib());
        assertNull(msg.getAnotherAttrib());

        msg = AnotherMessage.fromJSON(jsonWithUnknownProperties, AnotherMessage.class);
        assertEquals("1", msg.getOne());
        assertEquals("2", msg.getTwo());
        assertNull(msg.getSomeAttrib());
        assertNull(msg.getAnotherAttrib());

        // now we will fail on unknown properties
        SomeMessage.FAIL_ON_UNKNOWN_PROPERTIES = true;
        msg = AnotherMessage.fromJSON(jsonWithAllKnownProperties, AnotherMessage.class);
        assertEquals("1", msg.getOne());
        assertEquals("2", msg.getTwo());
        assertNull(msg.getSomeAttrib());
        assertNull(msg.getAnotherAttrib());

        try {
            msg = AnotherMessage.fromJSON(jsonWithUnknownProperties, AnotherMessage.class);
            fail("Custom mapper should not have been able to deserialize this.");
        } catch (Exception ok) {
        }
    }

}

class SomeMessage extends AbstractMessage {
    // we'll flip this in our tests
    public static boolean FAIL_ON_UNKNOWN_PROPERTIES = false;
    public static boolean SUPPORT_GETTER_SETTER = true;

    public String one;

    // this will be included in the JSON due to its getter/setter
    private String someAttrib;

    public SomeMessage() {
    }

    public SomeMessage(String one) {
        this.one = one;
    }

    public String getOne() {
        return this.one;
    }

    public String getSomeAttrib() {
        return this.someAttrib;
    }

    public void setSomeAttrib(String value) {
        this.someAttrib = value;
    }

    protected static ObjectMapper buildObjectMapperForDeserialization() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }

    @Override
    protected ObjectMapper buildObjectMapperForSerialization() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.PUBLIC_ONLY)
                .withGetterVisibility(
                        (SUPPORT_GETTER_SETTER)
                                ? JsonAutoDetect.Visibility.PUBLIC_ONLY
                                : JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(
                        (SUPPORT_GETTER_SETTER)
                                ? JsonAutoDetect.Visibility.PUBLIC_ONLY
                                : JsonAutoDetect.Visibility.NONE)
                .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
        return mapper;
    }
}

class AnotherMessage extends SomeMessage {

    public String two;

    // if our superclass supports getter/setter JSON, this will be included in the JSON
    private String anotherAttrib;

    public AnotherMessage() {
    }

    public AnotherMessage(String one, String two) {
        super(one);
        this.two = two;
    }

    public String getTwo() {
        return this.two;
    }

    public String getAnotherAttrib() {
        return this.anotherAttrib;
    }

    public void setAnotherAttrib(String value) {
        this.anotherAttrib = value;
    }
}
