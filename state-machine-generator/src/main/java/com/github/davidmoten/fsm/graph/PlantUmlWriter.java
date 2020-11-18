package com.github.davidmoten.fsm.graph;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PlantUmlWriter {

    public void print(PrintWriter out, Graph graph, Function<GraphNode, NodeOptions> options,
            boolean includeDocumentation) {

        out.println("@startuml");
        out.println();

        if (includeDocumentation) {
            graph //
                    .getNodes() //
                    .stream() //
                    .filter(x -> !x.state().isInitial()) //
                    .forEach(x -> {
                        Arrays.stream(x.state().documentation().orElse("").split("\n")) //
                                .map(y -> y.trim()) //
                                .map(y -> x.state().name() + ":" + y) //
                                .forEach(out::println);
                    });
        }

        graph //
                .getEdges() //
                .stream() //
                .forEach(x -> {
                    if (x.getFrom().state().isInitial()) {
                        out.println("[*] --> " + x.getTo().state().name() + " : "
                                + x.getTo().state().eventClass().getSimpleName());
                    } else {
                        out.println(x.getFrom().state().name() + " --> " + x.getTo().state().name()
                                + " : " + x.getTo().state().eventClass().getSimpleName());
                    }
                });

        out.println();
        out.println("@enduml");

    }

}
