package com.github.davidmoten.fsm.maven;

import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "generate-beans", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class BeanGeneratorMojo extends AbstractMojo {

    @Parameter(name = "sources", required = true)
    List<String> sources;

    @Override
    public void execute() throws MojoExecutionException {
    }
}
