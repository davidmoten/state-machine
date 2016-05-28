package com.github.davidmoten.fsm.example.account.event;

import java.math.BigDecimal;

import com.github.davidmoten.guavamini.Preconditions;

public final class Deposit extends ChangeBalance{

	public Deposit(BigDecimal amount) {
		super(amount);
		Preconditions.checkArgument(amount.signum()==1, "amount must be greater than zero");
	}

}
