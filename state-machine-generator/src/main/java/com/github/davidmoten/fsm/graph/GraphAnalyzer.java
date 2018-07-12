package com.github.davidmoten.fsm.graph;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class GraphAnalyzer {

    Graph graph;
    Set<String> visited = new HashSet<>();

    public GraphAnalyzer(Graph graph) {
        this.graph = graph;
    }

    /**
     * Determine if an edge connects to the destination
     * @param edge The edge information to examine
     * @return the edge information if a connection is found, null otherwise
     */
    private GraphEdge visit(GraphEdge edge, String from, String destination) {

        String to = edge.getTo().state().name();

        System.out.println("visiting node " + from);

        if(from.equals(destination))
            return edge;

        if(visited.contains(to)) {
            return null;
        }

        visited.add(from);

        if(destination.equals(to)) {
            return edge;
        }

        for(GraphEdge next : getEdges(graph, to))  {
            GraphEdge found = visit(next, to, destination);
            if(found != null) {
                return found;
            }
        }

        return null;
    }

    /**
     * Get the edges in a graph which start from the specified node
     * @param graph
     * @param from
     * @return
     */
    List<GraphEdge> getEdges(Graph graph, String from) {
        return graph.getEdges()
            .stream()
            .filter(e -> e.getFrom().state().name().equals(from))
            .collect(Collectors.toList());
    }

    /**
     * Query whether a GraphNode of name `to` is reachable from
     * GraphNode of name `from`
     */
    public boolean isReachable(String from, String destination) {
        GraphEdge result = null;

        List<GraphEdge> edges = getEdges(graph, from);

        for(GraphEdge next : edges)  {
            result = visit(next, from, destination);
            if(result != null) {
                System.out.println("Found path from " + from + " to " + destination);
                visited.clear();
                return true;
            }
        }

        visited.clear();
        return false;
    }
}
