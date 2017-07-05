package com.github.davidmoten.fsm.example.shop.product;

import java.util.Optional;

public final class Product {

	public final String productId;
	public final String name;
	public final String description;

	public Product(String productId, String name, String description, Optional<String> supplierId) {
		this.productId = productId;
		this.name = name;
		this.description = description;
	}

}
