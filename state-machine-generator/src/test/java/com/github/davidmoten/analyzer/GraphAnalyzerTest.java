package com.github.davidmoten.analyzer;

import com.github.davidmoten.fsm.model.StateMachineDefinition;
import com.github.davidmoten.resources.analyzer.StateMachineDefinitions;
import com.github.davidmoten.resources.analyzer.execution.Execution;
import com.github.davidmoten.fsm.graph.Graph;
import com.github.davidmoten.fsm.graph.GraphAnalyzer;

import org.junit.Test;

public class GraphAnalyzerTest {

    @Test
    public void graphAnalyzertest() {

        StateMachineDefinition<Execution> stateMachineDefinition = StateMachineDefinitions.createVinDiscoveryStateMachine();

        Graph graph = stateMachineDefinition.getGraph();

        GraphAnalyzer analyzer = new GraphAnalyzer(graph);

        assert(analyzer.isReachable("IN_PROGRESS", "IN_PROGRESS"));
        assert(analyzer.isReachable("STARTED", "SUCCEEDED"));
        assert(analyzer.isReachable("IN_PROGRESS", "IN_PROGRESS"));
        assert(!analyzer.isReachable("IN_PROGRESS", "STARTED"));
        assert(analyzer.isReachable("STARTED", "SUCCEEDED"));

        assert(analyzer.isReachable("DOWNLOAD_QUEUED", "DOWNLOAD_TIMEOUT"));
        assert(analyzer.isReachable("DOWNLOAD_QUEUED", "SUCCEEDED"));
        assert(!analyzer.isReachable("SUCCEEDED", "DOWNLOAD_QUEUED"));
        // reachable by virtue of failure
        assert(analyzer.isReachable("DOWNLOAD_TIMEOUT", "DOWNLOAD_QUEUED"));
    }
}
