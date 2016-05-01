package com.github.davidmoten.graph;

public interface Mover {

	GraphLayout apply(Graph graph, GraphLayout layout, float gridSize);

}
