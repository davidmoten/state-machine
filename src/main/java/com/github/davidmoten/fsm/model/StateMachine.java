package com.github.davidmoten.fsm.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StateMachine<T> {

    private final Class<T> cls;
    private final List<Transition<?, ?>> transitions = new ArrayList<>();

    private StateMachine(Class<T> cls) {
        this.cls = cls;
    }

    public static <T> StateMachine<T> create(Class<T> cls) {
        return new StateMachine<T>(cls);
    }

    public Class<T> cls() {
        return cls;
    }

    public <R> State<R> state(String name, Class<? extends Event<R>> eventClass) {
        return new State<R>(this, name, eventClass);
    }

    public <R, S> StateMachine<T> addTransition(State<R> state, State<S> other) {
        transitions.add(new Transition<R, S>(state, other));
        return this;
    }

    public void generateClasses(File directory, String pkg) {
        new Generator<T>(this, directory, pkg).generate();
        // generate state machine wrapper `ShipStateMachine` for an object that
        // accepts events, implements transitions and runs onEntry procedures
        // constructor should have object as parameter and another parameter
        // which implements the generate behaviour interface for the object
        // class `ShipBehaviour` which is passed to

        // generate ShipStateMachine and ShipBehaviour

    }

    public List<Transition<?, ?>> transitions() {
        return transitions;
    }

}
