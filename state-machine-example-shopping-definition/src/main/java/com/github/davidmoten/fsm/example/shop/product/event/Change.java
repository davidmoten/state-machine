package com.github.davidmoten.fsm.example.shop.product.event;

import com.github.davidmoten.fsm.example.shop.product.Product;
import com.github.davidmoten.fsm.runtime.Event;

public final class Change implements Event<Product> {
	
    public final String name;
    public final String description;

    public Change(String name, String description) {
		this.name = name;
		this.description = description;
	}
}