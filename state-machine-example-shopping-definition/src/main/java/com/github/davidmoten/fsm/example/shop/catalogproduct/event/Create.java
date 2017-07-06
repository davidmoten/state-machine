package com.github.davidmoten.fsm.example.shop.catalogproduct.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
import com.github.davidmoten.fsm.runtime.Event;

public final class Create implements Event<CatalogProduct> {

    public final String catalogId;
    public final String productId;
    public final int quantity;

    @JsonCreator
    public Create(@JsonProperty("catalogId") String catalogId, @JsonProperty("productId") String productId,
            @JsonProperty("quantity") int quantity) {
        this.catalogId = catalogId;
        this.productId = productId;
        this.quantity = quantity;
    }

}
