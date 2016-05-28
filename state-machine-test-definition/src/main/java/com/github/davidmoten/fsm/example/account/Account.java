package com.github.davidmoten.fsm.example.account;

import java.math.BigDecimal;

public final class Account {

	public final String id;
	public final BigDecimal balance;

	public Account(String id, BigDecimal balance) {
		this.id = id;
		this.balance = balance;
	}
	
}
