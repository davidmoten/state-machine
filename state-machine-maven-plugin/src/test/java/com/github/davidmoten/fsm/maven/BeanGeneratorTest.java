package com.github.davidmoten.fsm.maven;

import java.io.File;
import java.util.Date;

import org.junit.Test;

public final class BeanGeneratorTest {

    public static final class Example {
        String id;
        int number;
        Date[] values;
    }
    
    @Test
    public void testGenerate() {
        new BeanGenerator().generate(Example.class, new File("target"));
    }
}
