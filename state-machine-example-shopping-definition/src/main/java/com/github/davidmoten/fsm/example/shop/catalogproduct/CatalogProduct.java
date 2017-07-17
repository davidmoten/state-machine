package com.github.davidmoten.fsm.example.shop.catalogproduct;

import java.math.BigDecimal;
import java.util.List;

import com.github.davidmoten.bean.annotation.GenerateImmutable;

@GenerateImmutable
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

    public CatalogProduct(String catalogId, String productId, String name, String description, BigDecimal price,
            List<String> tags, int quantity) {
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
