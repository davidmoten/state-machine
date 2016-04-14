package com.github.davidmoten.fsm.model;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.davidmoten.fsm.runtime.Event;
import com.github.davidmoten.fsm.runtime.EventVoid;
import com.github.davidmoten.guavamini.Preconditions;

public final class StateMachine<T> {

    private final Class<T> cls;
    private final List<Transition<?, ?>> transitions = new ArrayList<>();
    private final Set<State<?>> states = new HashSet<State<?>>();
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
        State<R> state = new State<R>(this, name, eventClass);
        states.add(state);
        return state;
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
        states.add(initialState);
        return this;
    }

    public void generateClasses(File directory, String pkg) {
        new Generator<T>(this, directory, pkg).generate();
    }

    public List<Transition<?, ?>> transitions() {
        return transitions;
    }

    public String documentationHtml() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(bytes);
        out.println("<html/>");
        out.println("<body>");
        // states
        out.println("<h2>States</h2>");
        states.stream().map(state -> state.name()).sorted()
                .forEach(state -> out.println("<p class=\"state\"><b>" + state + "</b></p>"));

        // events
        out.println("<h2>Events</h2>");
        states.stream().map(state -> state.eventClass().getSimpleName()).distinct().sorted()
                .forEach(state -> out.println("<p class=\"state\"><b>" + state + "</b></p>"));

        // transition table
        // state onEntry template

        out.println("</body>");
        out.println("</html>");
        out.close();
        return new String(bytes.toByteArray(), StandardCharsets.UTF_8);
    }

    public boolean hasCreationTransition() {
        return transitions().stream().filter(t -> t.from().isInitial()).findAny().isPresent();
    }

}
