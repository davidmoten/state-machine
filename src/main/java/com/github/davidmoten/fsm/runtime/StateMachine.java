package com.github.davidmoten.fsm.runtime;

import com.github.davidmoten.fsm.model.Event;

public class StateMachine<T> {

    public final T object;

    public StateMachine(T object) {
        this.object = object;
    }

    public StateMachine<T> event(Event<?> event) {
        return this;
    }
}
