package com.github.davidmoten.fsm.example.microwave;

public final class Microwave {

    private final int serialNumber;

    public Microwave(int serialNumber) {
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
