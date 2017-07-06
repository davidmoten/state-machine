package com.github.davidmoten.fsm.persistence;

import java.util.Collections;
import java.util.List;

import com.github.davidmoten.guavamini.Lists;

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

    public static List<Property> list(String key, String value) {
        return Collections.singletonList(Property.create(key, value));
    }

}
