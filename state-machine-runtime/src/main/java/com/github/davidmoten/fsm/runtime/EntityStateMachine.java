package com.github.davidmoten.fsm.runtime;

public interface EntityStateMachine<T> extends ObjectState<T> {

	boolean transitionOccurred();
	
	EntityStateMachine<T> event(Event<?> event);
}
