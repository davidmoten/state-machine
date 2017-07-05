package com.github.davidmoten.fsm.example.shop.product;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Product {

	public final String productId;
	public final String name;
	public final String description;

	@JsonCreator
	public Product(@JsonProperty("productId") String productId, @JsonProperty("name") String name,
			@JsonProperty("description") String description) {
		this.productId = productId;
		this.name = name;
		this.description = description;
	}

}
