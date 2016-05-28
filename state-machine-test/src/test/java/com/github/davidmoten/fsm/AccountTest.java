package com.github.davidmoten.fsm;

import java.math.BigDecimal;

import org.junit.Test;

import com.github.davidmoten.fsm.example.account.Account;
import com.github.davidmoten.fsm.example.account.event.ChangeBalance;
import com.github.davidmoten.fsm.example.account.event.Deposit;
import com.github.davidmoten.fsm.example.account.event.Transfer;
import com.github.davidmoten.fsm.example.account.event.Withdrawal;
import com.github.davidmoten.fsm.example.generated.AccountBehaviourBase;
import com.github.davidmoten.fsm.example.generated.AccountStateMachine;
import com.github.davidmoten.fsm.runtime.Create;
import com.github.davidmoten.fsm.runtime.Signaller;
import com.github.davidmoten.fsm.runtime.rx.Processor;

import rx.schedulers.Schedulers;

public class AccountTest {

	@Test
	public void testAccount() throws InterruptedException {
		AccountBehaviourBase<String> behaviour = new AccountBehaviourBase<String>() {

			@Override
			public AccountStateMachine<String> create(String id) {
				return AccountStateMachine.create(id, this);
			}

			@Override
			public Account onEntry_Created(Signaller<Account, String> signaller, String id, Create event,
					boolean replaying) {
				return new Account(id, BigDecimal.ZERO);
			}

			@Override
			public Account onEntry_Changed(Signaller<Account, String> signaller, Account account, String id,
					ChangeBalance event, boolean replaying) {
				return new Account(id, account.balance.add(event.change));
			}

			@Override
			public Account onEntry_Transferred(Signaller<Account, String> signaller, Account account, String id,
					Transfer event, boolean replaying) {
				signaller.signalToSelf(new Withdrawal(event.amount));
				signaller.signal(Account.class, event.toAccountId, new Deposit(event.amount));
				return account;
			}

		};
		Processor<String> processor = Processor //
				.behaviour(Account.class, behaviour) //
				.processingScheduler(Schedulers.computation()) //
				.build();

		processor.observable() //
		        .doOnNext(System.out::println) //
				.subscribeOn(Schedulers.io()) //
				.subscribe();
		
		processor.signal(Account.class, "1", new Create());
		processor.signal(Account.class, "1", new Deposit(BigDecimal.valueOf(100)));
		
		Thread.sleep(1000);

	}

}
