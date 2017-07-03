package com.github.davidmoten.fsm.example.shop.basket;

import java.util.ArrayList;
import java.util.List;

public final class Basket {

    public final String id;
    public final List<BasketProduct> items;

    public Basket(String id, List<BasketProduct> items) {
        this.id = id;
        this.items = new ArrayList<>(items);
    }

}
