package com.github.davidmoten.fsm.runtime;

import com.github.davidmoten.fsm.model.Event;

public class Created implements Event<Void> {

    private static final Created instance = new Created();

    public static Created instance() {
        return instance;
    }

    @Override
    public Void value() {
        return null;
    }

}
