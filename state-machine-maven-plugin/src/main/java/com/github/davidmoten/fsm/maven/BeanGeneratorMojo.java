package com.github.davidmoten.fsm.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.github.davidmoten.bean.BeanGenerator;

@Mojo(name = "bean-generate", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class BeanGeneratorMojo extends AbstractMojo {

    @Parameter(name = "packageToScan", required = true)
    String packageToScan;

    @Parameter(name = "outputSourceDirectory", required = true)
    File outputSourceDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        BeanGenerator.scanAndGenerate(packageToScan, outputSourceDirectory);
    }
}
