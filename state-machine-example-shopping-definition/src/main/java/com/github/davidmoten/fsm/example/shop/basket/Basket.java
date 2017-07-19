package com.github.davidmoten.fsm.example.shop.basket;

import java.util.List;

import com.github.davidmoten.bean.annotation.GenerateImmutable;

@GenerateImmutable
public class Basket {

    String id;
    List<BasketProduct> items;

}
