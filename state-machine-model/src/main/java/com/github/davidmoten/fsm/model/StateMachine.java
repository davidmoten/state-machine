package com.github.davidmoten.fsm.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.github.davidmoten.fsm.runtime.Event;
import com.github.davidmoten.fsm.runtime.EventVoid;
import com.github.davidmoten.guavamini.Preconditions;

public final class StateMachine<T> {

    private final Class<T> cls;
    private final List<Transition<?, ?>> transitions = new ArrayList<>();
    private final State<Void> initialState;

    private StateMachine(Class<T> cls) {
        this.cls = cls;
        this.initialState = new State<Void>(this, "Initial", EventVoid.class);
    }

    public static <T> StateMachine<T> create(Class<T> cls) {
        return new StateMachine<T>(cls);
    }

    public Class<T> cls() {
        return cls;
    }

    public <R> State<R> state(String name, Class<? extends Event<R>> eventClass) {
        Preconditions.checkNotNull(name);
        if (name.equals("Initial")) {
            name = name.concat("_1");
        }
        return new State<R>(this, name, eventClass);
    }

    public <R, S> StateMachine<T> addTransition(State<R> state, State<S> other) {
        Transition<R, S> transition = new Transition<R, S>(state, other);
        System.out.println("adding " + transition);
        for (Transition<?, ?> t : transitions) {
            if (t.from() == state && t.to() == other) {
                throw new IllegalArgumentException(
                        "the transition already exists: " + state.name() + " -> " + other.name());
            }
        }
        transitions.add(transition);
        return this;
    }

    <S> StateMachine<T> addInitialTransition(State<S> other) {
        Transition<Void, S> transition = new Transition<Void, S>(initialState, other);
        System.out.println("adding " + transition);
        transitions.add(transition);
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
