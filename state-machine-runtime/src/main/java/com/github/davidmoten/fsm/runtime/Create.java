package com.github.davidmoten.fsm.runtime;

public final class Create implements Event<Object> {

    private static final Create instance = new Create();

    public static Create instance() {
        return instance;
    }

}
