package com.github.davidmoten.fsm.runtime;

import com.github.davidmoten.fsm.model.Event;

public final class EventVoid implements Event<Void>{

	@Override
	public Void value() {
		return null;
	}

}
