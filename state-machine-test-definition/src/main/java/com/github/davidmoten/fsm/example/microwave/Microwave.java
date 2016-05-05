package com.github.davidmoten.fsm.example.microwave;

public class Microwave {

    private final String id;

    public Microwave(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static Microwave fromId(String id) {
        return new Microwave(id);
    }

    @Override
    public String toString() {
        return "Microwave [id=" + id + "]";
    }

}
