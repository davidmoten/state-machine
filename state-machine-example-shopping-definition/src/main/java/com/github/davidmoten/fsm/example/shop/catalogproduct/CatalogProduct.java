package com.github.davidmoten.fsm.example.shop.catalogproduct;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class CatalogProduct {

    public final String catalogId;
    public final String productId;
    public final String name;
    public final String description;
    public final String quantity;

    @JsonCreator
    public CatalogProduct(@JsonProperty("catalogId") String catalogId, @JsonProperty("productId") String productId,
            @JsonProperty("name") String name, @JsonProperty("description") String description,
            @JsonProperty("quantity") String quantity) {
        this.catalogId = catalogId;
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.quantity = quantity;
    }

}
