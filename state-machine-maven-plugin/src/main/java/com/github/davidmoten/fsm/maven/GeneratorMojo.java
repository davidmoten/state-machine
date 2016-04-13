package com.github.davidmoten.fsm.maven;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.davidmoten.fsm.model.StateMachine;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GeneratorMojo extends AbstractMojo {

    @Parameter(name = "supplierClass", required = true)
    String supplierClass;

    @Parameter(name = "packageName", required = true)
    String packageName;

    @Parameter(name = "outputDirectory", defaultValue = "${project.build.directory}/generated-sources/java")
    File outputDirectory;

    @SuppressWarnings("unchecked")
    @Override
    public void execute() throws MojoExecutionException {
        Supplier<List<StateMachine<?>>> supplier;
        try {
            supplier = (Supplier<List<StateMachine<?>>>) (Class.forName(supplierClass)
                    .newInstance());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        List<StateMachine<?>> machines = supplier.get();
        for (StateMachine<?> machine : machines) {
            machine.generateClasses(outputDirectory, packageName);
        }
        getLog().info("generated classes in " + outputDirectory + " with package " + packageName);
    }
}