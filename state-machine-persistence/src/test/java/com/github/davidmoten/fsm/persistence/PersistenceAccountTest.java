package com.github.davidmoten.fsm.persistence;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.Callable;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import com.github.davidmoten.fsm.example.account.Account;
import com.github.davidmoten.fsm.example.account.event.ChangeBalance;
import com.github.davidmoten.fsm.example.account.event.Deposit;
import com.github.davidmoten.fsm.example.account.event.Transfer;
import com.github.davidmoten.fsm.example.account.event.Withdrawal;
import com.github.davidmoten.fsm.example.generated.AccountBehaviour;
import com.github.davidmoten.fsm.example.generated.AccountBehaviourBase;
import com.github.davidmoten.fsm.example.generated.AccountStateMachine;
import com.github.davidmoten.fsm.runtime.Create;
import com.github.davidmoten.fsm.runtime.EntityBehaviour;
import com.github.davidmoten.fsm.runtime.Signaller;
import com.github.davidmoten.fsm.runtime.TestExecutor;

public class PersistenceAccountTest {

    @Test
    public void testJsonSerializeAccountRoundTrip() {
        byte[] bytes = Serializer.JSON.serialize(new Account("dave", BigDecimal.TEN));
        System.out.println(new String(bytes));
        Account a = Serializer.JSON.deserialize(Account.class, bytes);
        Assert.assertEquals("dave", a.id);
        Assert.assertEquals(10, a.balance.intValue());
    }

    @Test
    public void testJsonSerializeTransferRoundTrip() {
        byte[] bytes = Serializer.JSON.serialize(new Transfer(BigDecimal.valueOf(12), "dave"));
        System.out.println(new String(bytes));
        Transfer a = Serializer.JSON.deserialize(Transfer.class, bytes);
        Assert.assertEquals("dave", a.toAccountId);
        Assert.assertEquals(12, a.amount.longValue());
    }

    @Test
    public void test() throws IOException {
        File directory = File.createTempFile("db-", "", new File("target"));
        directory.mkdir();
        Function<Class<?>, EntityBehaviour<?, String>> behaviourFactory = cls -> behaviour;
        TestExecutor executor = new TestExecutor();

        Callable<Connection> connectionFactory = () -> DriverManager
                .getConnection("jdbc:h2:" + directory.getAbsolutePath());
        Persistence p = Persistence //
                .connectionFactory(connectionFactory) //
                .executor(executor) //
                .behaviourFactory(behaviourFactory) //
                .build();
        p.create();
        p.initialize();
        Assert.assertFalse(p.get(Account.class, "1").isPresent());
        p.signal(Account.class, "1", new Create());
        Assert.assertEquals(BigDecimal.ZERO, p.get(Account.class, "1").get().entity.balance);
        p.signal(Account.class, "1", new Deposit(BigDecimal.valueOf(100)));
        Assert.assertEquals(BigDecimal.valueOf(100), p.get(Account.class, "1").get().entity.balance);
        p.signal(Account.class, "1", new Transfer(BigDecimal.valueOf(12), "2"));
        Assert.assertEquals(BigDecimal.valueOf(88), p.get(Account.class, "1").get().entity.balance);
        Assert.assertEquals(BigDecimal.valueOf(12), p.get(Account.class, "2").get().entity.balance);
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
            System.out.println("change=" + event.change);
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
