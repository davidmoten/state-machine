package com.github.davidmoten.fsm.graph;

import java.awt.Color;

public final class GraphOptions {

    public final float nodeWidth;
    public final float nodeHeight;
    public final Color backgroundColor;

    private static final GraphOptions defaultInstance = new GraphOptions(280.0f, 150f,
            // light yellow
            Color.decode("#F3F2C0"));

    public static GraphOptions defaultInstance() {
        return defaultInstance;
    }

    public GraphOptions(float nodeWidth, float nodeHeight, Color backgroundColor) {
        this.nodeWidth = nodeWidth;
        this.nodeHeight = nodeHeight;
        this.backgroundColor = backgroundColor;
    }

}
