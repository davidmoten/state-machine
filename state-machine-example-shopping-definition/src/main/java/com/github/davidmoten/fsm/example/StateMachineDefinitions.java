package com.github.davidmoten.fsm.example;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.github.davidmoten.fsm.example.account.Account;
import com.github.davidmoten.fsm.example.account.event.ChangeBalance;
import com.github.davidmoten.fsm.example.account.event.Transfer;
import com.github.davidmoten.fsm.example.microwave.Microwave;
import com.github.davidmoten.fsm.example.microwave.event.ButtonPressed;
import com.github.davidmoten.fsm.example.microwave.event.DoorClosed;
import com.github.davidmoten.fsm.example.microwave.event.DoorOpened;
import com.github.davidmoten.fsm.example.microwave.event.TimerTimesOut;
import com.github.davidmoten.fsm.example.shop.basket.Basket;
import com.github.davidmoten.fsm.example.shop.basket.event.BasketSelectionChange;
import com.github.davidmoten.fsm.example.shop.basket.event.Checkout;
import com.github.davidmoten.fsm.example.shop.basket.event.Clear;
import com.github.davidmoten.fsm.example.shop.basket.event.Payment;
import com.github.davidmoten.fsm.example.shop.basket.event.Timeout;
import com.github.davidmoten.fsm.model.State;
import com.github.davidmoten.fsm.model.StateMachineDefinition;
import com.github.davidmoten.fsm.runtime.Create;

public final class StateMachineDefinitions implements Supplier<List<StateMachineDefinition<?>>> {

    @Override
    public List<StateMachineDefinition<?>> get() {
        return Arrays.asList(createMicrowaveStateMachine(), createAccountStateMachine(), createBasketStateMachine());

    }

    private static StateMachineDefinition<Microwave> createMicrowaveStateMachine() {
        StateMachineDefinition<Microwave> m = StateMachineDefinition.create(Microwave.class);
        State<Microwave, DoorClosed> readyToCook = m.createState("Ready to Cook", DoorClosed.class)
                .documentation("<pre>entry/\nturn light off;</pre>");
        State<Microwave, DoorOpened> doorOpen = m.createState("Door Open", DoorOpened.class)
                .documentation("<pre>entry/\nturn light on;</pre>");
        State<Microwave, ButtonPressed> cooking = m.createState("Cooking", ButtonPressed.class)
                .documentation("<pre>entry/\nturn light on;\nsignal TimerTimesOut to self in 1 min;</pre>");
        State<Microwave, DoorOpened> cookingInterruped = m.createState("Cooking Interrupted", DoorOpened.class)
                .documentation("<pre>entry/\nturn light on;\ncancel signal to self;</pre>");
        State<Microwave, TimerTimesOut> cookingComplete = m.createState("Cooking Complete", TimerTimesOut.class)
                .documentation("<pre>entry/\nturn light off;</pre>");

        readyToCook.to(cooking).to(cookingInterruped)
                .to(readyToCook.from(doorOpen.from(readyToCook).from(cookingComplete.from(cooking))));

        return m;
    }

    private static StateMachineDefinition<Account> createAccountStateMachine() {
        StateMachineDefinition<Account> m = StateMachineDefinition.create(Account.class);
        State<Account, Create> created = m.createState("Created", Create.class);
        State<Account, ChangeBalance> changed = m.createState("Changed", ChangeBalance.class);
        State<Account, Transfer> transferred = m.createState("Transferred", Transfer.class);
        created.initial().to(changed).to(changed).to(transferred).to(changed);
        return m;
    }

    private static StateMachineDefinition<Basket> createBasketStateMachine() {
        StateMachineDefinition<Basket> m = StateMachineDefinition.create(Basket.class);
        State<Basket, Create> created = m.createState("Created", Create.class);
        State<Basket, Clear> empty = m.createState("Empty", Clear.class);
        State<Basket, BasketSelectionChange> changed = m.createState("Changed", BasketSelectionChange.class);
        State<Basket, Checkout> checkedOut = m.createState("CheckedOut", Checkout.class);
        State<Basket, Payment> paid = m.createState("Paid", Payment.class);
        State<Basket, Timeout> timedOut = m.createState("TimedOut", Timeout.class);
        created.initial() //
                .to(empty.from(changed).from(checkedOut).from(timedOut)) //
                .to(changed) //
                .to(checkedOut) //
                .to(paid);
        return m;
    }
}
