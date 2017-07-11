package com.github.davidmoten.fsm.example.shop.catalog.event;

import java.math.BigDecimal;

import com.github.davidmoten.fsm.example.shop.catalog.Catalog;
import com.github.davidmoten.fsm.runtime.Event;

public final class Change implements Event<Catalog> {

    public final String productId;
    public final int quantityDelta;
    public final BigDecimal price;

    public Change( String productId,  BigDecimal price,
             int quantityDelta) {
        this.productId = productId;
        this.price = price;
        this.quantityDelta = quantityDelta;
    }

    @Override
    public String toString() {
        return "Change [productId=" + productId + ", quantityDelta=" + quantityDelta + "]";
    }

}
