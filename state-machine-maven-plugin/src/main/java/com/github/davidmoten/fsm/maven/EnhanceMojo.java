package com.github.davidmoten.fsm.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.davidmoten.bean.BeanGenerator;

@Mojo(name = "enhance", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class EnhanceMojo extends AbstractMojo {

    @Parameter(name = "sourceDirectory", required = true)
    File sourceDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        BeanGenerator.scanAndGenerate(sourceDirectory);
    }
}
