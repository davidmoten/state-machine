package com.github.davidmoten.fsm.model;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class StateMachine {

    private final String className;
    private final Set<Transition<?, ?>> transitions = new HashSet<>();

    private StateMachine(String className) {
        this.className = className;
    }

    public static StateMachine create(String className) {
        return new StateMachine(className);
    }

    public String className() {
        return className;
    }

    public <T> State<T> state(String name, Class<? extends Event<T>> eventClass) {
        return new State<T>(this, name, eventClass);
    }

    public <T, R> StateMachine addTransition(State<T> state, State<R> other) {
        transitions.add(new Transition<T, R>(state, other));
        return this;
    }

    public void generateClasses(File directory, String pkg) {
        // generate state machine wrapper `ShipStateMachine` for an object that
        // accepts events, implements transitions and runs onEntry procedures
        // constructor should have object as parameter and another parameter
        // which implements the generate behaviour interface for the object
        // class `ShipBehaviour` which is passed to

        // generate ShipStateMachine and ShipBehaviour
    }

}
