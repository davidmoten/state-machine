package com.github.davidmoten.fsm.model;

import java.io.File;
import java.util.Optional;

import com.github.davidmoten.fsm.runtime.Event;

public final class State<T> {

    private final String name;
    private final Class<? extends Event<T>> eventClass;
    private final StateMachine<?> m;
    private boolean isCreationDestination;
    private Optional<String> documentation = Optional.empty();

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

    public Optional<String> documentation() {
        return documentation;
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
        isCreationDestination = true;
        m.addInitialTransition(this);
        return this;
    }

    public State<T> documentation(String html) {
        this.documentation = Optional.of(html);
        return this;
    }

    public boolean isCreationDestination() {
        return isCreationDestination;
    }

    public boolean isInitial() {
        return name().equals("Initial");
    }

    public void generateClasses(File directory, String pkg) {
        m.generateClasses(directory, pkg);
    }

}
