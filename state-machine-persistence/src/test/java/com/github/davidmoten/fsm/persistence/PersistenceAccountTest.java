package com.github.davidmoten.fsm.persistence;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.function.Function;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidmoten.fsm.example.account.Account;
import com.github.davidmoten.fsm.example.account.event.ChangeBalance;
import com.github.davidmoten.fsm.example.account.event.Deposit;
import com.github.davidmoten.fsm.example.account.event.Transfer;
import com.github.davidmoten.fsm.example.account.event.Withdrawal;
import com.github.davidmoten.fsm.example.generated.AccountBehaviour;
import com.github.davidmoten.fsm.example.generated.AccountBehaviourBase;
import com.github.davidmoten.fsm.example.generated.AccountStateMachine;
import com.github.davidmoten.fsm.runtime.ClockDefault;
import com.github.davidmoten.fsm.runtime.Create;
import com.github.davidmoten.fsm.runtime.EntityBehaviour;
import com.github.davidmoten.fsm.runtime.Signaller;

public class PersistenceAccountTest {

    @Test
    public void test() throws IOException {
        File directory = File.createTempFile("db-", "", new File("target"));
        directory.mkdir();
        Serializer entitySerializer = Serializer.JSON;
        Serializer eventSerializer = Serializer.JSON;
        Function<Class<?>, EntityBehaviour<?, String>> behaviourFactory = cls -> behaviour;
        TestExecutor executor = new TestExecutor();

        PersistenceH2 p = new PersistenceH2(directory, executor, ClockDefault.instance(), entitySerializer,
                eventSerializer, behaviourFactory);
    }

    private static final AccountBehaviour<String> behaviour = new AccountBehaviourBase<String>() {

        @Override
        public AccountStateMachine<String> create(String id) {
            System.out.println("created empty state machine");
            return AccountStateMachine.create(id, this);
        }

        @Override
        public Account onEntry_Created(Signaller<Account, String> signaller, String id, Create event,
                boolean replaying) {
            System.out.println("created event");
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

}
