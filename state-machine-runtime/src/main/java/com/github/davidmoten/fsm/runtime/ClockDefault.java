package com.github.davidmoten.fsm.runtime;

public class ClockDefault implements Clock{
	
	private static final ClockDefault instance = new ClockDefault();
	
	public static ClockDefault instance() {
		return instance;
	}

	@Override
	public long now() {
		return System.currentTimeMillis();
	}

}
