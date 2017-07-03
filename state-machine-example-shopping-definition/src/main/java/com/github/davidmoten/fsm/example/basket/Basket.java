package com.github.davidmoten.fsm.example.basket;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Basket {

    public final List<BasketProduct> items;

    @JsonCreator
    public Basket(@JsonProperty("items") List<BasketProduct> items) {
        this.items = new ArrayList<>(items);
    }

}
