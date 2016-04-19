package com.github.davidmoten.fsm.runtime;

public interface Context<Id> {
	
	<T> EntityStateMachine<T> get(Class<T> cls, Id id);
	
	void eventToSelf(Event<?> event);
	
}
