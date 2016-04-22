package com.github.davidmoten.fsm.runtime;

public final class Signal<T, R> {

	private final T object;
	private final Event<R> event;

	public Signal(T object, Event<R> event) {
		this.object = object;
		this.event = event;
	}

	public static <T, R> Signal<T, R> create(T object, Event<R> event) {
		return new Signal<T,R>(object, event);
	}

	public T object() {
		return object;
	}

	public Event<R> event() {
		return event;
	}

}
