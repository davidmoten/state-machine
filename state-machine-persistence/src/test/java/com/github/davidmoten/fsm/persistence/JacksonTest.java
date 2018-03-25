package com.github.davidmoten.fsm.persistence;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class JacksonTest {

    public static final class Example {
        public final String a;
        public final int b;

        public Example(String a, int b) {
            this.a = a;
            this.b = b;
        }
    }

    @Test
    public void testSerialize() {
        byte[] bytes = Serializer.JSON.serialize(new Example("boo", 123));
        System.out.println(new String(bytes));
        Example e = Serializer.JSON.deserialize(Example.class, bytes);
        assertEquals("boo", e.a);
        assertEquals(123, e.b);
    }

}
