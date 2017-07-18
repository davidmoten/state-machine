package com.github.davidmoten.bean;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Test;

import com.github.davidmoten.bean.annotation.GenerateImmutable;

public final class BeanGeneratorTest {

    @Test
    public void testGenerate() {
        BeanGenerator.generate(Example.class, "test.immutable", new File("target"));
    }

    @Test
    public void testAnnotationFound() {
        Assert.assertTrue(Example.class.isAnnotationPresent(GenerateImmutable.class));
    }

    /**
     * @throws IOException
     */
    @Test
    public void testJavaParser() throws IOException {
        String code = new String(
                Files.readAllBytes(new File("src/test/java/com/github/davidmoten/bean/Example.java").toPath()),
                StandardCharsets.UTF_8);
        BeanGenerator.generate(code, "test2.immutable", new File("target"));
    }
}
