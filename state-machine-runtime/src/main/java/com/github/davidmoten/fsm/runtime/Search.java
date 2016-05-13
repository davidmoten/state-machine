package com.github.davidmoten.fsm.runtime;

import java.util.Optional;

@FunctionalInterface
public interface Search<Id> {

    <T> Optional<T> search(Class<T> cls, Id id);

}
