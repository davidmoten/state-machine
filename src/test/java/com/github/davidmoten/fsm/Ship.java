package com.github.davidmoten.fsm;

import java.util.Optional;

public class Ship {

    private final Optional<String> imo;
    private final Optional<String> mmsi;
    private final float lat;
    private final float lon;

    private Ship(Optional<String> imo, Optional<String> mmsi, float lat, float lon) {
        this.imo = imo;
        this.mmsi = mmsi;
        this.lat = lat;
        this.lon = lon;
    }

    public Optional<String> getImo() {
        return imo;
    }

    public Optional<String> getMmsi() {
        return mmsi;
    }

    public float getLat() {
        return lat;
    }

    public float getLon() {
        return lon;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Optional<String> imo;
        private Optional<String> mmsi;
        private float lat;
        private float lon;

        private Builder() {
        }

        public Builder imo(Optional<String> imo) {
            this.imo = imo;
            return this;
        }

        public Builder mmsi(Optional<String> mmsi) {
            this.mmsi = mmsi;
            return this;
        }

        public Builder lat(float lat) {
            this.lat = lat;
            return this;
        }

        public Builder lon(float lon) {
            this.lon = lon;
            return this;
        }

        public Ship build() {
            return new Ship(imo, mmsi, lat, lon);
        }
    }

}
