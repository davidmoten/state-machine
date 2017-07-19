package com.github.davidmoten.fsm.example.shop.basket;

import com.github.davidmoten.bean.annotation.GenerateImmutable;

@GenerateImmutable
public final class BasketProduct {

    String basketId;
    String productId;
    int quantity;

}
