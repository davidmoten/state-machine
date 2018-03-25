package com.github.davidmoten.fsm.example.shop.product;

import java.util.List;

import com.github.davidmoten.bean.annotation.GenerateImmutable;

@GenerateImmutable
public class Product {

    String productId;
    String name;
    String description;
    List<String> tags;

}
