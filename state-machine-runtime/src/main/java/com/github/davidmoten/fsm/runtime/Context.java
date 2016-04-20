package com.github.davidmoten.fsm.runtime;

public interface Context {
	
	<T> void event(T object, Event<?> event);
	
	void eventToSelf(Event<?> event);
	
}
