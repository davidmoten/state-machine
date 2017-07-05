package com.github.davidmoten.fsm.example.shop.product;

public final class Product {

	public final String productId;
	public final String name;
	public final String description;

	public Product(String productId, String name, String description) {
		this.productId = productId;
		this.name = name;
		this.description = description;
	}

}
