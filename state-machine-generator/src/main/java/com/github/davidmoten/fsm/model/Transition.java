package com.github.davidmoten.fsm.model;

public final class Transition<A, B> {
    private final State<A> from;
    private final State<B> to;

    public Transition(State<A> from, State<B> to) {
        this.from = from;
        this.to = to;
    }

    public State<A> from() {
        return from;
    }

    public State<B> to() {
        return to;
    }

    @Override
    public String toString() {
        return "Transition [" + from.name() + " -> " + to.name() + "]";
    }

}
