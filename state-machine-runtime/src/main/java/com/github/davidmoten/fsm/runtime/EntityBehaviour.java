package com.github.davidmoten.fsm.runtime;

public interface EntityBehaviour<T, Id> {

    EntityStateMachine<T, Id> create(Id id);

    EntityStateMachine<T, Id> create(Id id, T entity, EntityState<T> state);
    
    EntityState<T> from(String name);

}
