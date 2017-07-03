package com.github.davidmoten.fsm.example.product;

import java.util.Optional;

public final class Product {

    public final String name;
    public final String description;
    public final Optional<String> supplierId;

    public Product(String name, String description, Optional<String> supplierId) {
        this.name = name;
        this.description = description;
        this.supplierId = supplierId;
    }

}
