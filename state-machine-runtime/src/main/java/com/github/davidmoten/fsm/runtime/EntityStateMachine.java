package com.github.davidmoten.fsm.runtime;

import java.util.List;

public interface EntityStateMachine<T> extends ObjectState<T> {

    boolean transitionOccurred();

    EntityStateMachine<T> signal(Event<? super T> event);

    List<Event<? super T>> signalsToSelf();

    List<Signal<?, ?>> signalsToOther();

    Class<T> cls();
}
