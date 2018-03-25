package com.github.davidmoten.fsm.example.shop.order;

import java.math.BigDecimal;
import java.util.List;

public final class Order {

    public final String id;
    public final String customerId;
    public final String address;
    public final String phoneNumber;

    public final List<OrderItem> items;
    public final BigDecimal shippingCost;
    public final BigDecimal discount;

    public Order(String id, String customerId, String address, String phoneNumber, List<OrderItem> items,
            BigDecimal shippingCost, BigDecimal discount) {
        this.id = id;
        this.customerId = customerId;
        this.address = address;
        this.phoneNumber = phoneNumber;
        this.items = items;
        this.shippingCost = shippingCost;
        this.discount = discount;
    }

}
