package com.github.davidmoten.fsm.runtime;

public final class CancelTimedSignal implements Event<CancelTimedSignal> {

	private final Object from;

	public CancelTimedSignal(Object from) {
		this.from = from;
	}

	public Object from() {
		return from;
	}
}
