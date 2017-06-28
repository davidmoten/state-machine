package com.github.davidmoten.fsm.persistence;

import com.github.davidmoten.fsm.runtime.EntityStateMachine;
import com.github.davidmoten.fsm.runtime.Signal;
import com.github.davidmoten.fsm.runtime.rx.ClassId;

public interface Persistence<T> {

    EntityStateMachine<T, String> process(EntityStateMachine<T, String> esm, Serializer<? super T> serializer,
            Signal<T, String> signal);

    EntityStateMachine<T, String> replay(ClassId<T, String> classId);

}
