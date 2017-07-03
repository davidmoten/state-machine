package com.github.davidmoten.fsm.example.product;

import java.util.Optional;
import java.util.function.Supplier;

public final class Product {

    public final String name;
    public final String description;
    public final Optional<Supplier> supplier;
    public Product(String name, String description, Optional<Supplier> supplier) {
        super();
        this.name = name;
        this.description = description;
        this.supplier = supplier;
    }
    
    
    
}
