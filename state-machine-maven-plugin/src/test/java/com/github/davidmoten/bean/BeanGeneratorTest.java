package com.github.davidmoten.bean;

import java.io.File;

import org.junit.Test;

import com.github.davidmoten.bean.BeanGenerator;

public final class BeanGeneratorTest {

    
    @Test
    public void testGenerate() {
        new BeanGenerator().generate(Example.class, new File("target"));
    }
}
