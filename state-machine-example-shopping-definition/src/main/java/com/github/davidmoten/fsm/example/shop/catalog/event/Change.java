package com.github.davidmoten.fsm.example.shop.catalog.event;

import com.github.davidmoten.fsm.example.shop.catalog.Catalog;
import com.github.davidmoten.fsm.runtime.Event;

public final class Change implements Event<Catalog>{

	public final String productId;
	public final int quantityDelta;

	public Change(String productId, int quantityDelta) {
		this.productId = productId;
		this.quantityDelta = quantityDelta;
	}

}
