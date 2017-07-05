package com.github.davidmoten.fsm.example.shop.catalog;

import java.util.List;

public final class Catalog {

	public final List<CatalogProduct> products;

	public Catalog(List<CatalogProduct> products) {
		this.products = products;
	}
}
