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

import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BasicMessageObjectMapperTest {

    private static JsonMapper jsonMapper;

    private static ObjectMapper mapper;

    @BeforeClass
    public static void init() {
        jsonMapper = new JsonMapper();
        jsonMapper.init();

        mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setVisibilityChecker(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                        .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                        .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                        .withSetterVisibility(JsonAutoDetect.Visibility.NONE)
                        .withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
    }

    @Test
    public void testWithGetterSetterSupport() throws Exception {
        SomeMessage.FAIL_ON_UNKNOWN_PROPERTIES = true;
        SomeMessage.SUPPORT_GETTER_SETTER = true;

        AnotherMessage msg = new AnotherMessage("1", "2");
        msg.setSomeAttrib("someValue");
        msg.setAnotherAttrib("anotherValue");
        assertNotNull(msg.getOne());
        assertNotNull(msg.getTwo());
        assertNotNull(msg.getSomeAttrib());
        assertNotNull(msg.getAnotherAttrib());

        String json = mapper.writeValueAsString(msg);
        System.out.println(json);
        assertNotNull("missing JSON", json);

        AnotherMessage msg2 = jsonMapper.toBasicMessage(json, AnotherMessage.class);
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

//    @Test
    public void testWithoutGetterSetterSupport() throws Exception {
        SomeMessage.FAIL_ON_UNKNOWN_PROPERTIES = true;
        SomeMessage.SUPPORT_GETTER_SETTER = false;

        AnotherMessage msg = new AnotherMessage("1", "2");
        msg.setSomeAttrib("someValueNoSupport");
        msg.setAnotherAttrib("anotherValueNoSupport");
        assertNotNull(msg.getOne());
        assertNotNull(msg.getTwo());
        assertNotNull(msg.getSomeAttrib());
        assertNotNull(msg.getAnotherAttrib());

        String json = mapper.writeValueAsString(msg);
        System.out.println(json);
        assertNotNull("missing JSON", json);

        AnotherMessage msg2 = jsonMapper.toBasicMessage(json, AnotherMessage.class);
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
        msg = jsonMapper.toBasicMessage(jsonWithAllKnownProperties, AnotherMessage.class);
        assertEquals("1", msg.getOne());
        assertEquals("2", msg.getTwo());
        assertNull(msg.getSomeAttrib());
        assertNull(msg.getAnotherAttrib());

        msg = jsonMapper.toBasicMessage(jsonWithUnknownProperties, AnotherMessage.class);
        assertEquals("1", msg.getOne());
        assertEquals("2", msg.getTwo());
        assertNull(msg.getSomeAttrib());
        assertNull(msg.getAnotherAttrib());

        // now we will fail on unknown properties
        SomeMessage.FAIL_ON_UNKNOWN_PROPERTIES = true;
        msg = jsonMapper.toBasicMessage(jsonWithAllKnownProperties, AnotherMessage.class);
        assertEquals("1", msg.getOne());
        assertEquals("2", msg.getTwo());
        assertNull(msg.getSomeAttrib());
        assertNull(msg.getAnotherAttrib());
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
