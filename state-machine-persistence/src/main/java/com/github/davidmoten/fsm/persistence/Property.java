package com.github.davidmoten.fsm.persistence;

import java.util.ArrayList;
import java.util.List;

import com.github.davidmoten.guavamini.Preconditions;

public final class Property {

    private final String name;
    private final String value;

    private Property(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String name() {
        return name;
    }

    public String value() {
        return value;
    }

    public static Property create(String key, String value) {
        return new Property(key, value);
    }

    public static List<Property> list(String... items) {
        Preconditions.checkArgument(items.length % 2 == 0);
        List<Property> list = new ArrayList<>();
        for (int i = 0; i < items.length / 2; i++) {
            list.add(Property.create(items[2 * i], items[2 * i + 1]));
        }
        return list;
    }

}
