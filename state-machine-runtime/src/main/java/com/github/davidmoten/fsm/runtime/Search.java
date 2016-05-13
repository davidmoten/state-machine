package com.github.davidmoten.fsm.runtime;

public interface Search<Id> {

    <T> T search(Class<T> cls, Id id);

}
