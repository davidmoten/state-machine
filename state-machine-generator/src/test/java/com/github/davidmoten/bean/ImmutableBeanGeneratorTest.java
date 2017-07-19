package com.github.davidmoten.bean;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Assert;
import org.junit.Test;

import com.github.davidmoten.bean.annotation.GenerateImmutable;

public final class ImmutableBeanGeneratorTest {

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
        System.out.println(ImmutableBeanGenerator.generate(code).generatedCode());
    }

    @Test
    public void testScanAndGenerate() {
        ImmutableBeanGenerator.scanAndGenerate(new File("src/test/java"), new File("target/gen"));
        assertTrue(new File("target/gen/com/github/davidmoten/bean/immutable/Example.java").exists());
    }
}
