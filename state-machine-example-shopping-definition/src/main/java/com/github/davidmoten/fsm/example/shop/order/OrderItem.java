package com.github.davidmoten.fsm.example.shop.order;

import java.math.BigDecimal;

public final class OrderItem {

    public final String id;
    public final String productId;
    public final int quantity;
    public final BigDecimal cost;

    public OrderItem(String id, String productId, int quantity, BigDecimal cost) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.cost = cost;
    }

}
