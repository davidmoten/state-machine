package com.github.davidmoten.fsm.graph;

public class GraphNode {

    private final String name;
    private final boolean enabled;

    public GraphNode(String name, boolean enabled) {
        this.name = name;
        this.enabled = enabled;
    }

    public String name() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return name;
    }

}
