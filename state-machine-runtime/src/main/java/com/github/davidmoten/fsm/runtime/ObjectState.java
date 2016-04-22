package com.github.davidmoten.fsm.runtime;

import java.util.Optional;

public interface ObjectState<T> {

	Optional<T> get();

	EntityState<T> state();

}
