package com.github.davidmoten.fsm.example.shop.catalogproduct.event;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
import com.github.davidmoten.fsm.runtime.Event;

public final class Create implements Event<CatalogProduct> {

    public final String catalogId;
    public final String productId;
    public final int quantity;
    public final BigDecimal price;

    @JsonCreator
    public Create(@JsonProperty("catalogId") String catalogId, @JsonProperty("productId") String productId,
            @JsonProperty("price") BigDecimal price, @JsonProperty("quantity") int quantity) {
        this.catalogId = catalogId;
        this.productId = productId;
        this.price = price;
        this.quantity = quantity;
    }

}
