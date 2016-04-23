package com.github.davidmoten.fsm.runtime;

import java.util.concurrent.TimeUnit;

import com.github.davidmoten.util.Preconditions;

public final class Signal<T, R> {

	private final T object;
	private final Event<R> event;
	private final long delay;
	private final TimeUnit unit;

	private Signal(T object, Event<R> event, long delay, TimeUnit unit) {
		Preconditions.checkArgument(delay >= 0, "delay must be non-negative");
		Preconditions.checkNotNull(unit, "unit cannot be null");
		this.object = object;
		this.event = event;
		this.delay = delay;
		this.unit = unit;
	}

	public static <T, R> Signal<T, R> create(T object, Event<R> event) {
		return new Signal<T, R>(object, event, 0, TimeUnit.MILLISECONDS);
	}

	public static <T, R> Signal<T, R> create(T object, Event<R> event, long delay, TimeUnit unit) {
		return new Signal<T, R>(object, event, delay, unit);
	}

	public T object() {
		return object;
	}

	public Event<R> event() {
		return event;
	}

	public long delay() {
		return delay;
	}

	public TimeUnit unit() {
		return unit;
	}

}
