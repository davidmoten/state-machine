package com.github.davidmoten.fsm.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.davidmoten.bean.ImmutableBeanGenerator;

@Mojo(name = "generate-immutable", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class ImmutableBeanGeneratorMojo extends AbstractMojo {
    

    @Parameter(name = "sourceDirectory", required = true)
    File sourceDirectory;
    
    @Parameter(name = "generatedSourceDirectory", required = true)
    File generatedSourceDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        
        
        ImmutableBeanGenerator.scanAndGenerate(sourceDirectory, generatedSourceDirectory);
    }

}
