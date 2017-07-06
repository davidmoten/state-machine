package com.github.davidmoten.fsm.example.shop.catalogproduct.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.davidmoten.fsm.example.shop.catalog.Catalog;
import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
import com.github.davidmoten.fsm.runtime.Event;

public final class Change implements Event<CatalogProduct> {

    public final int quantityDelta;

    @JsonCreator
    public Change(@JsonProperty("quantityDelta") int quantityDelta) {
        this.quantityDelta = quantityDelta;
    }

}
