package com.github.davidmoten.fsm.graph;

public class GraphNode {

    private final String name;
    private final String descriptionHtml;

    public GraphNode(String name, String descriptionHtml) {
        this.name = name;
        this.descriptionHtml = descriptionHtml;
    }

    public String name() {
        return name;
    }

    public String descriptionHtml() {
        return descriptionHtml;
    }

    @Override
    public String toString() {
        return name;
    }

}
