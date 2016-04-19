package com.github.davidmoten.fsm.runtime;

import java.util.function.Function;

public interface Entities<Id> {

	<T> void register(Class<T> cls, Function<T, Id> id);
	
	<T> EntityStateMachine<T> lookup(Class<T> cls, Id id);
	
	<T> void event(Class<T> cls, Id id, Event<?> event);

}
