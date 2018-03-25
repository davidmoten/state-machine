package com.github.davidmoten.fsm.example.shop.basket;

import java.math.BigDecimal;

import com.github.davidmoten.bean.annotation.GenerateImmutable;
import com.github.davidmoten.fsm.example.shop.customer.Customer;
import com.github.davidmoten.fsm.runtime.Event;

@GenerateImmutable
public class Change implements Event<Basket> {

    String productId;
    int quantity;
    BigDecimal unitCost;

}
