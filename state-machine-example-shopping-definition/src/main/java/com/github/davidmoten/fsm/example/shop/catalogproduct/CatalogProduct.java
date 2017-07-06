package com.github.davidmoten.fsm.example.shop.catalogproduct;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class CatalogProduct {

    public final String catalogId;
    public final String productId;
    // Note that we have a copy of the Product attributes for query purposes
    public final String name;
    public final String description;
    public final int quantity;

    @JsonCreator
    public CatalogProduct(@JsonProperty("catalogId") String catalogId, @JsonProperty("productId") String productId,
            @JsonProperty("name") String name, @JsonProperty("description") String description,
            @JsonProperty("quantity") int quantity) {
        this.catalogId = catalogId;
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.quantity = quantity;
    }

    public static String idFrom(String catalogId, String productId) {
        return catalogId + "|" + productId;
    }
    

}
