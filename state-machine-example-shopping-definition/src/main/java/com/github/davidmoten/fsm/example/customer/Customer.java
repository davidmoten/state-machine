package com.github.davidmoten.fsm.example.customer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.davidmoten.guavamini.Preconditions;

public final class Customer {

    public final String name;
    public final String email;

    @JsonCreator
    public Customer(@JsonProperty("name") String name, @JsonProperty("email") String email) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(email);
        this.name = name;
        this.email = email;
    }

}
