package com.github.davidmoten.fsm.example.shop.catalog;

import java.util.Map;

public final class Catalog {

	/**
	 * Products by productId.
	 */
	public final Map<String, CatalogProduct> products;

	public Catalog(Map<String, CatalogProduct> products) {
		this.products = products;
	}
}
