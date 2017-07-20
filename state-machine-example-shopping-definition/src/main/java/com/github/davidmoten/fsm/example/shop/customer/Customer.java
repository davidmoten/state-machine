package com.github.davidmoten.fsm.example.shop.customer;

import com.github.davidmoten.bean.annotation.GenerateImmutable;
import com.github.davidmoten.guavamini.Preconditions;

@GenerateImmutable
public final class Customer {

    String id;
    String name;
    String email;

}
