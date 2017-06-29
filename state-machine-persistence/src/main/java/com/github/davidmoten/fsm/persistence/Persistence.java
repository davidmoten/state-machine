package com.github.davidmoten.fsm.persistence;

import java.util.Optional;

public interface Persistence {

    void offer(NumberedSignal<?, String> signal);

    void process(NumberedSignal<?, String> signal);

    <T> Optional<T> get(Class<T> cls, String id);

    void replay(Class<?> cls, String id);

    Serializer entitySerializer();

    Serializer eventSerializer();

}
