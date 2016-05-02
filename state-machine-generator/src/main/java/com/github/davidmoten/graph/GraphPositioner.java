package com.github.davidmoten.graph;

public interface GraphPositioner {

	GraphLayout apply(Graph graph, GraphLayout layout, float gridSize);

}
