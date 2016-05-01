package com.github.davidmoten.graph;

import java.util.Map;

public class GraphLayout {

	public final Map<Node, Position> nodePositions;
	public final Map<Edge, Position> edgePositions;

	public GraphLayout(Map<Node, Position> nodePositions, Map<Edge, Position> edgePositions) {
		this.nodePositions = nodePositions;
		this.edgePositions = edgePositions;
	}

}
