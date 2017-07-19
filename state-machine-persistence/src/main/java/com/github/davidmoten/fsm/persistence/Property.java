package com.github.davidmoten.fsm.persistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public static Property create(String name, String value) {
        return new Property(name, value);
    }

    public static List<Property> list(String name, Collection<String> values) {
        return values.stream() //
                .map(x -> Property.create(name, x)) //
                .collect(Collectors.toList());
    }

    public static List<Property> list(String... items) {
        Preconditions.checkArgument(items.length % 2 == 0);
        List<Property> list = new ArrayList<>();
        for (int i = 0; i < items.length / 2; i++) {
            list.add(Property.create(items[2 * i], items[2 * i + 1]));
        }
        return list;
    }

    public static List<Property> concatenate(@SuppressWarnings("unchecked") List<Property>... lists) {
        List<Property> list = new ArrayList<>();
        for (List<Property> x : lists) {
            list.addAll(x);
        }
        return list;
    }

    public static String combineNames(String... names) {
        return combineNames(Arrays.stream(names));
    }

    public static String combineNames(List<String> names) {
        return combineNames(names.stream());
    }

    public static String combineNames(Stream<String> names) {
        return names.collect(Collectors.joining("|"));
    }

}
