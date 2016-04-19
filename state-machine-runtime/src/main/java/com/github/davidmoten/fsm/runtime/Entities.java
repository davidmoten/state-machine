package com.github.davidmoten.fsm.runtime;

import java.util.function.Function;

public interface Entities<Id> {

	<T> void identifier(Class<T> cls, Function<T, Id> id);
	
	<T> T lookup(Class<T> cls, Id id);
	
	
}
