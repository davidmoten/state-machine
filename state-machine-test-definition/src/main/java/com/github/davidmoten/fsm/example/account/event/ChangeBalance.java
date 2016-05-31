package com.github.davidmoten.fsm.example.account.event;

import java.math.BigDecimal;

import com.github.davidmoten.fsm.example.account.Account;
import com.github.davidmoten.fsm.runtime.Event;

public class ChangeBalance implements Event<Account> {

	public final BigDecimal change;

	public ChangeBalance(BigDecimal change) {
		this.change = change;
	}
	
	
}
