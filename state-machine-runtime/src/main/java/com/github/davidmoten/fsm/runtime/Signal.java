package com.github.davidmoten.fsm.runtime;

public final class Signal<T, R> {

	private final T object;
	private final Event<R> event;
	private long time;

	private Signal(T object, Event<R> event, long time) {
		this.object = object;
		this.event = event;
		this.time = time;
	}

	public static <T, R> Signal<T, R> create(T object, Event<R> event) {
		return new Signal<T, R>(object, event, Long.MIN_VALUE);
	}

	public static <T, R> Signal<T, R> create(T object, Event<R> event, long time) {
		return new Signal<T, R>(object, event, time);
	}

	public T object() {
		return object;
	}

	public Event<R> event() {
		return event;
	}

	public long time() {
		return time;
	}

	public boolean isImmediate() {
		return time == Long.MIN_VALUE;
	}

	public Signal<?, ?> now() {
		return create(object, event);
	}

	@Override
	public String toString() {
		return "Signal [object=" + object + ", event=" + event + ", time=" + (isImmediate() ? "immediate" : time) + "]";
	}

}
