package com.github.davidmoten.fsm.graph;

import com.github.davidmoten.fsm.model.State;

public final class GraphNode {

    private final State<?> state;

    public GraphNode(State<?> state) {
        this.state = state;
    }

    public State<?> state() {
        return state;
    }

    @Override
    public String toString() {
        return state.name();
    }

}
