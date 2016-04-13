package com.github.davidmoten.fsm.example;

import com.github.davidmoten.fsm.runtime.Event;

public class Out implements Event<Out> {
    public final float lat;
    public final float lon;

    public Out(float lat, float lon) {
        this.lat = lat;
        this.lon = lon;
    }

}