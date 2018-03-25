package com.github.davidmoten.fsm.example.shop.catalog;

import java.math.BigDecimal;

import com.github.davidmoten.bean.annotation.GenerateImmutable;
import com.github.davidmoten.fsm.runtime.Event;

@GenerateImmutable
public class Change implements Event<Catalog> {

    String productId;
    int quantityDelta;
    BigDecimal price;

}
