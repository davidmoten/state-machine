package com.github.davidmoten.fsm.example.shop.product;

import java.util.List;

public final class Product {

    public final String productId;
    public final String name;
    public final String description;
    public final List<String> tags;

    public Product(String productId, String name, String description, List<String> tags) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.tags = tags;
    }

}
