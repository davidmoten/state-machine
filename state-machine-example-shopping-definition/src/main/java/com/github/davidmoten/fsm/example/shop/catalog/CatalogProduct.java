package com.github.davidmoten.fsm.example.shop.catalog;

public final class CatalogProduct {

    public final String id;
    public final String productId;
    public final String quantity;

    public CatalogProduct(String id, String productId, String quantity) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
    }

}
