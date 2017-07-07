package com.github.davidmoten.fsm.persistence;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class PropertyTest {

    @Test
    public void testList() {
        List<Property> list = Property.list("a", "0", "b", "1");
        assertEquals(2, list.size());
        assertEquals("a", list.get(0).name());
        assertEquals("0", list.get(0).value());
        assertEquals("b", list.get(1).name());
        assertEquals("1", list.get(1).value());
    }
}
