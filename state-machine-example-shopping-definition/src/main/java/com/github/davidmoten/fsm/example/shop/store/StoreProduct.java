package com.github.davidmoten.fsm.example.shop.store;

public final class StoreProduct {

    public final String id;
    public final String productId;
    public final String quantity;

    public StoreProduct(String id, String productId, String quantity) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
    }

}
