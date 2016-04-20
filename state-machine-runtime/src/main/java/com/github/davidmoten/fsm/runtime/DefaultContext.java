package com.github.davidmoten.fsm.runtime;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class DefaultContext<T> implements Context {

	private final Map<T, EntityStateMachine<?>> stateMachines = new ConcurrentHashMap<>();
	private final Map<Class<?>, Function<?, ? extends T>> keys = new ConcurrentHashMap<>();
	private final Map<Class<?>, Function<?, EntityStateMachine<?>>> factories = new ConcurrentHashMap<>();

	public <R> void registerKeyFunction(Class<R> cls, Function<? super R, ? extends T> f) {
		keys.put(cls, f);
	}

	@SuppressWarnings("unchecked")
	public <R, S extends EntityStateMachine<R>> void registerStateMachineSupplier(Class<R> cls,
			Function<? super R, ? extends EntityStateMachine<R>> factory) {
		factories.put(cls, (Function<?, EntityStateMachine<?>>) (Function<?, ?>) factory);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> void signal(R object, Event<?> event) {
		Function<R, T> f = (Function<R, T>) keys.get(object.getClass());
		if (f == null) {
			throw new RuntimeException("not key function registerd for class " + object.getClass().getName());
		} else {
			T key = f.apply(object);
			EntityStateMachine<?> m = stateMachines.get(key);
			if (m == null) {
				Function<R, EntityStateMachine<R>> factory = ((Function<R, EntityStateMachine<R>>) (Function<R, ?>) factories
						.get(object.getClass()));
				m = factory.apply(object);
			}
			m.event(event);
		}
	}

	@Override
	public void signalToSelf(Event<?> event) {
		// TODO Auto-generated method stub

	}

}
