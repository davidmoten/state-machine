package com.github.davidmoten.graph;

import java.util.List;

public class Graph {

	public final List<Node> nodes;
	public final List<Edge> edges;

	public Graph(List<Node> nodes, List<Edge> edges) {
		this.nodes = nodes;
		this.edges = edges;
	}

}
