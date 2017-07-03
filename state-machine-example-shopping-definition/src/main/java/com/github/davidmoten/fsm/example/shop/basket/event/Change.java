package com.github.davidmoten.fsm.example.shop.basket.event;

import java.math.BigDecimal;

import com.github.davidmoten.fsm.example.shop.basket.Basket;
import com.github.davidmoten.fsm.runtime.Event;

public final class Change implements Event<Basket>{

    public final String productId;
    public final int quantity;
    public final BigDecimal unitCost;
    
    public Change(String productId, int quantity, BigDecimal unitCost) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitCost = unitCost;
    }
    
}
