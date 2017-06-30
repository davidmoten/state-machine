package com.github.davidmoten.fsm.persistence;

import java.util.Optional;

import com.github.davidmoten.fsm.persistence.PersistenceH2.EntityAndState;

public interface Persistence {

    void offer(NumberedSignal<?, String> signal);

    <T> Optional<EntityAndState<T>> get(Class<T> cls, String id);

    void replay(Class<?> cls, String id);

    Serializer entitySerializer();

    Serializer eventSerializer();

}
