package com.github.davidmoten.fsm.example.shop.product.event;

import com.github.davidmoten.fsm.example.shop.product.Product;
import com.github.davidmoten.fsm.runtime.Event;

public class Create implements Event<Product>{
	
	public final String productId;
	public final String name;
	public final String description;

	public Create(String productId, String name, String description) {
		this.productId = productId;
		this.name = name;
		this.description = description;
	}
}
