package com.github.davidmoten.fsm.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "generate")
public class GeneratorMojo extends AbstractMojo {
    public void execute() throws MojoExecutionException {
        getLog().info("Hello, world.");
    }
}