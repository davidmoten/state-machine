package com.github.davidmoten.fsm.example.account.event;

import java.math.BigDecimal;

import com.github.davidmoten.fsm.example.account.Account;
import com.github.davidmoten.fsm.runtime.Event;

public final class Transfer implements Event<Account> {

    public final BigDecimal amount;
    public final String toAccountId;

    public Transfer(BigDecimal amount, String toAccountId) {
        this.amount = amount;
        this.toAccountId = toAccountId;
    }

}
