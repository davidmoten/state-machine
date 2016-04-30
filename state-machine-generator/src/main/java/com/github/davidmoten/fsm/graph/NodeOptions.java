package com.github.davidmoten.fsm.graph;

import java.awt.Color;

public final class NodeOptions {

    public final float nodeWidth;
    public final float nodeHeight;
    public final Color backgroundColor;

    private static final NodeOptions defaultInstance = new NodeOptions(280.0f, 150f,
            // light yellow
            Color.decode("#F3F2C0"));

    public static NodeOptions defaultInstance() {
        return defaultInstance;
    }

    public NodeOptions(float nodeWidth, float nodeHeight, Color backgroundColor) {
        this.nodeWidth = nodeWidth;
        this.nodeHeight = nodeHeight;
        this.backgroundColor = backgroundColor;
    }

}
