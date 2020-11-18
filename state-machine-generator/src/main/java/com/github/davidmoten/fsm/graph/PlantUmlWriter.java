package com.github.davidmoten.fsm.graph;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.function.Function;

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
                        Arrays.stream(x.state().documentation().orElse("").replaceAll("<.+>","").split("\n")) //
                                .map(y -> y.trim()) //
                                .map(y -> name(x.state().name()) + ":" + y) //
                                .forEach(out::println);
                    });
        }

        graph //
                .getEdges() //
                .stream() //
                .forEach(x -> {
                    if (x.getFrom().state().isInitial()) {
                        out.println("[*] --> " + name(x.getTo().state().name()) + " : "
                                + x.getTo().state().eventClass().getSimpleName());
                    } else {
                        out.println(name(x.getFrom().state().name()) + " --> " + name(x.getTo().state().name())
                                + " : " + x.getTo().state().eventClass().getSimpleName());
                    }
                });

        out.println();
        out.println("@enduml");

    }
    
    private static String name(String name) {
        return name.replaceAll(" ", "_");
    }

}
