package com.github.davidmoten.fsm.model;

import java.io.File;

import com.github.davidmoten.fsm.runtime.Event;

public final class State<T> {

    private final String name;
    private final Class<? extends Event<T>> eventClass;
    private final StateMachine<?> m;
    private boolean initial;

    public State(StateMachine<?> m, String name, Class<? extends Event<T>> eventClass) {
        this.m = m;
        this.name = name;
        this.eventClass = eventClass;
    }

    public Class<? extends Event<T>> eventClass() {
        return eventClass;
    }

    public String name() {
        return name;
    }

    public StateMachine<?> stateMachine() {
        return m;
    }

    public <R> State<R> to(State<R> other) {
        m.addTransition(this, other);
        return other;
    }

    public <R> State<T> from(State<R> other) {
        m.addTransition(other, this);
        return this;
    }

    public State<T> initial() {
        initial = true;
        m.addInitialTransition(this);
        return this;
    }

    public boolean isInitial() {
        return initial;
    }

    public void generateClasses(File directory, String pkg) {
        m.generateClasses(directory, pkg);
    }

}
