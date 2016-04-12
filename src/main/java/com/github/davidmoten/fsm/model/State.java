package com.github.davidmoten.fsm.model;

public class State<T> {

    private final String name;
    private final Class<? extends Event<T>> eventClass;
    private final StateMachine m;

    public State(StateMachine m, String name, Class<? extends Event<T>> eventClass) {
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

    public StateMachine stateMachine() {
        return m;
    }

    public void to(State<?> other) {
        m.addTransition(this, other);
    }

}
