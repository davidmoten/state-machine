package com.github.davidmoten.fsm.example.shop.product.event;

import java.util.List;

import com.github.davidmoten.fsm.example.shop.product.Product;
import com.github.davidmoten.fsm.runtime.Event;

public class Create implements Event<Product> {

    public final String productId;
    public final String name;
    public final String description;
    public final List<String> tags;

    public Create(String productId, String name, String description, List<String> tags) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.tags = tags;
    }
}
