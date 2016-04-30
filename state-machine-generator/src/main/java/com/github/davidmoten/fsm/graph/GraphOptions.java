package com.github.davidmoten.fsm.graph;

public final class GraphOptions {

    public final float nodeWidth;
    public final float nodeHeight;

    private static final GraphOptions defaultInstance = new GraphOptions(280.0f, 150f);

    public static GraphOptions defaultInstance() {
        return defaultInstance;
    }

    public GraphOptions(float nodeWidth, float nodeHeight) {
        this.nodeWidth = nodeWidth;
        this.nodeHeight = nodeHeight;
    }

}
