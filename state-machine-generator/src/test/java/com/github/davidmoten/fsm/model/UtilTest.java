package com.github.davidmoten.fsm.model;

import static com.github.davidmoten.fsm.model.Util.camelCaseToLowerUnderscore;
import static com.github.davidmoten.fsm.model.Util.toColumnName;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.github.davidmoten.junit.Asserts;

public class UtilTest {

    @Test
    public void testToTableName() {
        assertEquals("hello_there", camelCaseToLowerUnderscore("HelloThere"));
    }

    @Test
    public void testToTableNameSequenceOfCapitals() {
        assertEquals("hello_there", camelCaseToLowerUnderscore("HelloTHERE"));
    }

    @Test
    public void testToTableNameSequenceOfCapitalsFollowedByLowerCase() {
        assertEquals("hello_there", camelCaseToLowerUnderscore("HelloTHEre"));
    }

    @Test
    public void testToColumnName() {
        assertEquals("a", toColumnName("a"));
        assertEquals("ab", toColumnName("ab"));
        assertEquals("a_b", toColumnName("aB"));
        assertEquals("a_b", toColumnName("a_b"));
        assertEquals("a_b", toColumnName("a b"));
        assertEquals("a_b_c", toColumnName("a b c"));
        assertEquals("a_b_two", toColumnName("A B two"));
    }

    @Test
    public void testToJavaConstantIdentifierEndsWithDigit() {
        assertEquals("STATE1", Util.toJavaConstantIdentifier("State1"));
    }

    @Test
    public void testToJavaConstantIdentifierHasSpaces() {
        String s = Util.toJavaConstantIdentifier("State 1");
        System.out.println(s);
        assertEquals("STATE_1", s);
    }

    @Test
    public void testCamelCaseToUnderscoreForAllCapitalsJustConvertsLowerToUpper() {
        assertEquals("mmsi", Util.camelCaseToLowerUnderscore("MMSI"));
        assertEquals("id", Util.camelCaseToLowerUnderscore("ID"));
        assertEquals("i_d", Util.camelCaseToLowerUnderscore("I D"));
    }

    @Test
    public void isUtilityClass() {
        Asserts.assertIsUtilityClass(Util.class);
    }

}