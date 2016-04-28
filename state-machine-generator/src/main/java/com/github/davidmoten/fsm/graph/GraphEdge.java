package com.github.davidmoten.fsm.graph;

public class GraphEdge {

	private final GraphNode from;
	private final GraphNode to;

	public GraphEdge(GraphNode from, GraphNode to) {
		this.from = from;
		this.to = to;
	}

	public GraphNode getFrom() {
		return from;
	}

	public GraphNode getTo() {
		return to;
	}

	@Override
	public String toString() {
		return "";
	}

}
