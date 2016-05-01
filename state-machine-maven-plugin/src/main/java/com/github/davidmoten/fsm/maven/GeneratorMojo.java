package com.github.davidmoten.fsm.maven;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.Supplier;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.davidmoten.fsm.graph.NodeOptions;
import com.github.davidmoten.fsm.model.StateMachine;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GeneratorMojo extends AbstractMojo {

	@Parameter(name = "supplierClass", required = true)
	String supplierClass;

	@Parameter(name = "packageName", required = true)
	String packageName;

	@Parameter(name = "outputDirectory", defaultValue = "${project.build.directory}/generated-sources/java")
	File outputDirectory;

	@Parameter(name = "diagramsDirectory", defaultValue = "${project.build.directory}/state-machine-docs")
	File diagramsDirectory;

	@Parameter(name = "htmlDirectory", defaultValue = "${project.build.directory}/state-machine-docs")
	File htmlDirectory;

	@Parameter(name = "nodeWidth", defaultValue = "280")
	Float nodeWidth;

	@Parameter(name = "nodeHeight", defaultValue = "150")
	Float nodeHeight;

	@Parameter(name = "nodeBackgroundColor", defaultValue = "#")
	String nodeBackgroundColor;

	@SuppressWarnings("unchecked")
	@Override
	public void execute() throws MojoExecutionException {
		Supplier<List<StateMachine<?>>> supplier;
		try {
			supplier = (Supplier<List<StateMachine<?>>>) (Class.forName(supplierClass).newInstance());
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		List<StateMachine<?>> machines = supplier.get();
		for (StateMachine<?> machine : machines) {
			machine.generateClasses(outputDirectory, packageName);
			// with docs
			generateGraphml(machine, true);
			// without docs
			generateGraphml(machine, false);
			generateHtml(machine);
		}
		getLog().info("generated classes in " + outputDirectory + " with package " + packageName);
	}

	private void generateHtml(StateMachine<?> machine) {
		htmlDirectory.mkdirs();
		File gml = new File(htmlDirectory, machine.cls().getCanonicalName().replace("$", ".") + ".html");
		try (PrintWriter out = new PrintWriter(gml)) {
			out.println(machine.documentationHtml());
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		getLog().info("generated graphml file for import to yed (for instance): " + gml);

	}

	private void generateGraphml(StateMachine<?> machine, boolean includeDocumentation) {
		diagramsDirectory.mkdirs();
		File gml = new File(diagramsDirectory, machine.cls().getCanonicalName().replace("$", ".")
				+ (includeDocumentation ? "-with-docs" : "") + ".graphml");
		try (PrintWriter out = new PrintWriter(gml)) {
			out.println(
					machine.graphml((node -> new NodeOptions(nodeWidth, nodeHeight, Color.decode(nodeBackgroundColor))),
							includeDocumentation));
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		getLog().info("generated graphml file for import to yed (for instance): " + gml);
	}
}