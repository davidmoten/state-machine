package com.github.davidmoten.fsm.example.account.event;

import java.math.BigDecimal;

import com.github.davidmoten.guavamini.Preconditions;

public final class Withdrawal extends ChangeBalance{

	public Withdrawal(BigDecimal amount) {
		super(amount.negate());
		Preconditions.checkArgument(amount.signum()==1, "amount must be greater than zero");
	}

}
