package com.github.davidmoten.fsm.model;

import java.io.File;
import java.util.Optional;

import com.github.davidmoten.fsm.runtime.Event;

public final class State<T, R extends Event<? super T>> {

    private final String name;
    private final Class<R> eventClass;
    private final StateMachine<T> m;
    private boolean isCreationDestination;
    private Optional<String> documentation = Optional.empty();

    public State(StateMachine<T> m, String name, Class<R> eventClass) {
        this.m = m;
        this.name = name;
        this.eventClass = eventClass;
    }

    public Class<R> eventClass() {
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

    public <S extends Event<? super T>> State<T, S> to(State<T, S> other) {
        m.addTransition(this, other);
        return other;
    }

    public <S extends Event<? super T>> State<T, R> from(State<T, S> other) {
        m.addTransition(other, this);
        return this;
    }

    public State<T, R> initial() {
        isCreationDestination = true;
        m.addInitialTransition(this);
        return this;
    }

    public State<T, R> documentation(String html) {
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
