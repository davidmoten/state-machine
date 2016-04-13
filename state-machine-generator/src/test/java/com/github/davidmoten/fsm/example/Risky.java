package com.github.davidmoten.fsm.example;

import com.github.davidmoten.fsm.runtime.Event;

public class Risky implements Event<Risky> {
    public final String message;

    public Risky(String message) {
        this.message = message;
    }

}