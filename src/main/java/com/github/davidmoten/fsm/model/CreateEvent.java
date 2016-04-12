package com.github.davidmoten.fsm.model;

public class CreateEvent implements Event<Void> {

    private static final CreateEvent instance = new CreateEvent();

    public static CreateEvent instance() {
        return instance;
    }

    @Override
    public Void value() {
        return null;
    }

}
