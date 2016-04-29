package com.github.davidmoten.fsm.graph;

public class GraphNode {

    private final String name;

    public GraphNode(String name) {
        this.name = name;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

}
