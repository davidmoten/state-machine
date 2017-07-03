package com.github.davidmoten.fsm.example.account.event;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.davidmoten.fsm.example.account.Account;
import com.github.davidmoten.fsm.runtime.Event;

public final class Transfer implements Event<Account> {

    public final BigDecimal amount;
    public final String toAccountId;

    @JsonCreator
    public Transfer(@JsonProperty("amount") BigDecimal amount, @JsonProperty("toAccoundId") String toAccountId) {
        this.amount = amount;
        this.toAccountId = toAccountId;
    }

}
