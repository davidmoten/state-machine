package com.github.davidmoten.fsm.model;

import com.github.davidmoten.fsm.runtime.Event;

public final class Transition<T, A extends Event<? super T>, B extends Event<? super T>> {
    private final State<T, A> from;
    private final State<T, B> to;

    public Transition(State<T, A> from, State<T, B> to) {
        this.from = from;
        this.to = to;
    }

    public State<T, A> from() {
        return from;
    }

    public State<T, B> to() {
        return to;
    }

    @Override
    public String toString() {
        return "Transition [" + from.name() + " -> " + to.name() + "]";
    }

}
