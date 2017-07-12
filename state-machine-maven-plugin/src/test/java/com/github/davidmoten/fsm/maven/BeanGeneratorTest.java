package com.github.davidmoten.fsm.maven;

import java.io.File;
import java.util.Date;

import org.junit.Test;

public final class BeanGeneratorTest {

    
    @Test
    public void testGenerate() {
        new BeanGenerator().generate(Example.class, new File("target"));
    }
}
