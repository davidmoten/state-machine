package com.github.davidmoten.fsm.maven;

import java.io.File;

import org.junit.Test;

public final class BeanGeneratorTest {

    public static final class Example {
        String id;
        int number;
    }
    
    @Test
    public void testGenerate() {
        new BeanGenerator().generate(Example.class, new File("target"));
    }
}
