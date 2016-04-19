package com.github.davidmoten.fsm.runtime;

import java.util.Optional;

public interface EntityStateMachine<T> {

	Optional<T> get();
	
	EntityState<T> state();
	
	boolean transitionOccurred();
	
	EntityStateMachine<T> event(Event<?> event);
}
