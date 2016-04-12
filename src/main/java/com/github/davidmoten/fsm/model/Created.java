package com.github.davidmoten.fsm.model;

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
