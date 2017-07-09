package com.github.davidmoten.fsm.example.shop.catalogproduct;

import java.math.BigDecimal;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class CatalogProduct {

    public final String catalogId;
    public final String productId;
    // Note that we have a copy of the Product attributes for query performance
    // purposes
    public final String name;
    public final String description;
    public final int quantity;
    public final BigDecimal price;
    public final List<String> tags;

    @JsonCreator
    public CatalogProduct(@JsonProperty("catalogId") String catalogId, @JsonProperty("productId") String productId,
            @JsonProperty("name") String name, @JsonProperty("description") String description,
            @JsonProperty("price") BigDecimal price,
            @JsonProperty("tags") List<String> tags, @JsonProperty("quantity") int quantity) {
        this.catalogId = catalogId;
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.tags = tags;
        this.quantity = quantity;
    }
    
    public static String idFrom(String catalogId, String productId) {
        return catalogId + "|" + productId;
    }

}
