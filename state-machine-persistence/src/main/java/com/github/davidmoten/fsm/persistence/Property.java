package com.github.davidmoten.fsm.persistence;

public final class Property {

    private final String key;
    private final String value;

    private Property(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String key() {
        return key;
    }

    public String value() {
        return value;
    }

    public static Property create(String key, String value) {
        return new Property(key, value);
    }

}
