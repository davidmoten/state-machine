package com.github.davidmoten.fsm.graph;

public class GraphNode {

    private final String name;
    private final boolean enabled;
    private final String longName;

    public GraphNode(String name, String longName, boolean enabled) {
        this.name = name;
        this.longName = longName;
        this.enabled = enabled;
    }

    public String name() {
        return name;
    }

    public String longName() {
        return longName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return name;
    }

}
