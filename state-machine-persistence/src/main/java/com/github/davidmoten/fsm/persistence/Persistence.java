package com.github.davidmoten.fsm.persistence;

import com.github.davidmoten.fsm.runtime.EntityStateMachine;
import com.github.davidmoten.fsm.runtime.rx.ClassId;

public interface Persistence<T> {

    void persist(EntityStateMachine<T, String> esm, Serializer<? super T> serializer);

    EntityStateMachine<T, String> replay(ClassId<T, String> classId);
    
}
