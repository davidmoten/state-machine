package com.github.davidmoten.fsm.runtime;

public final class Create implements Event<Void> {

    private static final Create instance = new Create();

    public static Create instance() {
        return instance;
    }

}
