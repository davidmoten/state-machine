package com.github.davidmoten.fsm.example.shop.product.event;

import java.util.List;

import com.github.davidmoten.fsm.example.shop.product.Product;
import com.github.davidmoten.fsm.runtime.Event;

public final class ChangeDetails implements Event<Product> {

    public final String name;
    public final String description;
    public final List<String> tags;

    public ChangeDetails(String name, String description, List<String> tags) {
        this.name = name;
        this.description = description;
        this.tags = tags;
    }
}