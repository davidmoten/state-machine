package com.github.davidmoten.fsm.runtime;

public interface EntityBehaviour<T, Id> {

    EntityStateMachine<T, Id> create(Id id);

}
