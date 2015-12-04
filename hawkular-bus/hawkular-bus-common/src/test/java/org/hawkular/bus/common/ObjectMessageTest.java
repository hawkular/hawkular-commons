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

import org.junit.Assert;
import org.junit.Test;

public class ObjectMessageTest {

    @Test
    public void testObjectMessageNoArgConstructor() {
        ObjectMessage msg = new ObjectMessage();
        try {
            msg.getObject();
            Assert.fail("should have failed - didn't tell it the class");
        } catch (IllegalStateException expected) {
        }
        /*
            Jackson library needs quotes for single json string representation
         */
        msg.setMessage("\"foo\"");
        msg.setObjectClass(String.class);
        Object o = msg.getObject();
        Assert.assertEquals(String.class, o.getClass());
        Assert.assertEquals("foo", o.toString());
    }

    @Test
    public void testObjectMessage() {
        ObjectMessage msg = new ObjectMessage(new String("bar"));
        Object o = msg.getObject();
        Assert.assertEquals(String.class, o.getClass());
        Assert.assertEquals("bar", o.toString());
    }

    @Test
    public void testCustomObjectMessage() {
        MyObj myobj = new MyObj();
        myobj.letters = "abc";
        myobj.number = 123;

        // pass object to constructor
        ObjectMessage msg = new ObjectMessage(myobj);
        Object o = msg.getObject();
        Assert.assertEquals(MyObj.class, o.getClass());
        Assert.assertEquals("abc", ((MyObj) o).letters);
        Assert.assertEquals(123, ((MyObj) o).number);
        Assert.assertNotEquals(msg.toJSON(), msg.getMessage());

        // pass class to constructor
        msg = new ObjectMessage(MyObj.class);
        msg.setMessage("{\"letters\":\"xyz\",\"number\":987}}");
        Assert.assertEquals("xyz", ((MyObj) msg.getObject()).letters);
        Assert.assertEquals(987, ((MyObj) msg.getObject()).number);

        // pass nothing to constructor
        msg = new ObjectMessage();
        msg.setObjectClass(MyObj.class);
        msg.setMessage("{\"letters\":\"xzy\",\"number\":1987}}");
        Assert.assertEquals("xzy", ((MyObj) msg.getObject()).letters);
        Assert.assertEquals(1987, ((MyObj) msg.getObject()).number);
    }

    @Test
    public void testSerializingObjectMessageItself() {
        MyObj myobj = new MyObj();
        myobj.letters = "abc";
        myobj.number = 123;
        ObjectMessage msg = new ObjectMessage(myobj);
        String jsonPayload = msg.toJSON();

        ObjectMessage objectUnderTest = AbstractMessage.fromJSON(jsonPayload, ObjectMessage.class);
        objectUnderTest.setObjectClass(MyObj.class);
        Assert.assertEquals("abc", ((MyObj) objectUnderTest.getObject()).letters);
        Assert.assertEquals(123, ((MyObj) objectUnderTest.getObject()).number);
    }
}

class MyObj {
    public String letters;
    public int number;
}
