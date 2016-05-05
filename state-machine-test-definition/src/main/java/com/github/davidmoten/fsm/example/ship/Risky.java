package com.github.davidmoten.fsm.example.ship;

import com.github.davidmoten.fsm.runtime.Event;

public class Risky implements Event<Ship> {
    public final String message;

    public Risky(String message) {
        this.message = message;
    }

}