package com.github.davidmoten.fsm.persistence;

import java.util.Optional;

import com.github.davidmoten.fsm.runtime.Signal;

public interface Persistence {
    
    void offer(Signal<?,String> signal);

    void process(Signal<?, String> signal);

    <T> Optional<T> get(Class<T> cls, String id);

    void replay(Class<?> cls, String id);

    Serializer entitySerializer();

    Serializer eventSerializer();

}
