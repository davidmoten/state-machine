package com.github.davidmoten.fsm.model;

import java.io.File;

import com.github.davidmoten.fsm.runtime.Event;

public class State<T> {

    private final String name;
    private final Class<? extends Event<T>> eventClass;
    private final StateMachine<?> m;

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
    
    public State<T> initial() {
    	m.addInitialTransition(this);
    	return this;
    }

	public void generateClasses(File directory, String pkg) {
		m.generateClasses(directory, pkg);
	}

}
