package com.github.davidmoten.fsm.example.shop.customer;

import com.github.davidmoten.guavamini.Preconditions;

public final class Customer {

    public final String id;
    public final String name;
    public final String email;

    public Customer(String id, String name, String email) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(email);
        this.id = id;
        this.name = name;
        this.email = email;
    }

}
