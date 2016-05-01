package com.github.davidmoten.graph;

public class Edge {
	public final Node from;
	public final Node to;
	public final Rectangle label;

	public Edge(Node from, Node to, Rectangle label) {
		this.from = from;
		this.to = to;
		this.label = label;
	}

}
