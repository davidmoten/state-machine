package com.github.davidmoten.fsm.example.shop.basket.event;

import java.math.BigDecimal;

public final class RemoveItem {

    public final String productId;
    public final int quantity;
    public final BigDecimal unitCost;
    
    public RemoveItem(String productId, int quantity, BigDecimal unitCost) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }
    
}
