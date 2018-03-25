package com.github.davidmoten.fsm.example.microwave;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Microwave {

    private final int serialNumber;

    @JsonCreator
    public Microwave(@JsonProperty("serialNumber") int serialNumber) {
        this.serialNumber = serialNumber;
    }

    public int serialNumber() {
        return serialNumber;
    }

    public static String idFromSerialNumber(int n) {
        return String.valueOf(n);
    }

    public static Microwave fromId(String id) {
        return new Microwave(Integer.parseInt(id));
    }

    @Override
    public String toString() {
        return "Microwave [serialNumber=" + serialNumber + "]";
    }

}
