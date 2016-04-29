package com.github.davidmoten.fsm.graph;

public final class GraphEdge {

    private final GraphNode from;
    private final GraphNode to;
    private final String label;

    public GraphEdge(GraphNode from, GraphNode to, String label) {
        this.from = from;
        this.to = to;
        this.label = label;
    }

    public GraphNode getFrom() {
        return from;
    }

    public GraphNode getTo() {
        return to;
    }

    public String label() {
        return this.label;
    }

    @Override
    public String toString() {
        return "";
    }

}
