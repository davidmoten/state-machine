package com.github.davidmoten.fsm.example;

import com.github.davidmoten.fsm.runtime.Event;

public class In implements Event<In> {
    public final float lat;
    public final float lon;

    public In(float lat, float lon) {
        this.lat = lat;
        this.lon = lon;
    }

}