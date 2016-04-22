package com.github.davidmoten.fsm.runtime;

public final class Signal<T, R> {

	private final T object;
	private final Event<R> event;

	public Signal(T object, Event<R> event) {
		this.object = object;
		this.event = event;
	}

	public T object() {
		return object;
	}

	public Event<R> event() {
		return event;
	}

}
