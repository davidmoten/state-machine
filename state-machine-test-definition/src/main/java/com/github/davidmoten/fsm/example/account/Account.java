package com.github.davidmoten.fsm.example.account;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Account {

    public final String id;
    public final BigDecimal balance;

    @JsonCreator
    public Account(@JsonProperty("id") String id, @JsonProperty("balance") BigDecimal balance) {
        this.id = id;
        this.balance = balance;
    }
    
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("Account [id=");
        b.append(id);
        b.append(", balance=");
        b.append(balance);
        b.append("]");
        return b.toString();
    }

}
