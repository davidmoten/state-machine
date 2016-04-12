package com.github.davidmoten.fsm.runtime;

import com.github.davidmoten.fsm.model.Event;

public class Create implements Event<Void> {

    private static final Create instance = new Create();

    public static Create instance() {
        return instance;
    }

}
