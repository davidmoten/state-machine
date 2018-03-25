package com.github.davidmoten.fsm.example.shop.product;

import java.util.List;

import com.github.davidmoten.bean.annotation.GenerateImmutable;
import com.github.davidmoten.fsm.runtime.Event;

@GenerateImmutable
public class Create implements Event<Product> {

    String productId;
    String name;
    String description;
    List<String> tags;

}
