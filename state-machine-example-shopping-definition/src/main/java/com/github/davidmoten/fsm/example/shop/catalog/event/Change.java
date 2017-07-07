package com.github.davidmoten.fsm.example.shop.catalog.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.davidmoten.fsm.example.shop.catalog.Catalog;
import com.github.davidmoten.fsm.runtime.Event;

public final class Change implements Event<Catalog> {

    public final String productId;
    public final int quantityDelta;

    @JsonCreator
    public Change(@JsonProperty("productId") String productId, @JsonProperty("quantityDelta") int quantityDelta) {
        this.productId = productId;
        this.quantityDelta = quantityDelta;
    }

    @Override
    public String toString() {
        return "Change [productId=" + productId + ", quantityDelta=" + quantityDelta + "]";
    }

}
