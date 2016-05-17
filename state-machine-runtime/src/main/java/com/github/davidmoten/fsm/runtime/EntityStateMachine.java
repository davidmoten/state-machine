package com.github.davidmoten.fsm.runtime;

import java.util.List;
import java.util.Optional;

public interface EntityStateMachine<T, Id> extends ObjectState<T> {

    boolean transitionOccurred();

    Optional<? extends EntityState<T>> previousState();

    EntityStateMachine<T, Id> signal(Event<? super T> event);

    List<Event<? super T>> signalsToSelf();

    List<Signal<?, ?>> signalsToOther();

    Class<T> cls();

    EntityStateMachine<T, Id> withSearch(Search<Id> search);

    EntityStateMachine<T, Id> withAsyncSignaller(AsyncSignaller<Id> asyncSignaller);
}
