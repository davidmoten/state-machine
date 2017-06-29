package com.github.davidmoten.fsm.persistence;

import java.util.Optional;

import com.github.davidmoten.fsm.runtime.EntityStateMachine;
import com.github.davidmoten.fsm.runtime.Event;

public interface Persistence<T> {

    EntityStateMachine<T, String> process(EntityStateMachine<T, String> esm,
            Serializer serializer, Event<T> event,
            Serializer eventSerializer);

    Optional<T> get(Class<T> cls, String id);

    EntityStateMachine<T, String> replay(Class<T> cls, String id);

}
