package com.github.davidmoten.fsm.example.shop.catalogproduct;

import java.math.BigDecimal;
import java.util.List;

import com.github.davidmoten.bean.annotation.GenerateImmutable;

@GenerateImmutable
public final class CatalogProduct {

    String catalogId;
    String productId;
    // Note that we have a copy of the Product attributes for query performance
    // purposes
    String name;
    String description;
    int quantity;
    BigDecimal price;
    List<String> tags;

    public static String idFrom(String catalogId, String productId) {
        return catalogId + "|" + productId;
    }

}
