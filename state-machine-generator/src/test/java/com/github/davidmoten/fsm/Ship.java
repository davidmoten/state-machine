package com.github.davidmoten.fsm;

public class Ship {

    private final String imo;
    private final String mmsi;
    private final float lat;
    private final float lon;

    public Ship(String imo, String mmsi, float lat, float lon) {
        this.imo = imo;
        this.mmsi = mmsi;
        this.lat = lat;
        this.lon = lon;
    }

    public String imo() {
        return imo;
    }

    public String mmsi() {
        return mmsi;
    }

    public float lat() {
        return lat;
    }

    public float lon() {
        return lon;
    }

}
