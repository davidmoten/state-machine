package com.github.davidmoten.fsm.example;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

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
        return Arrays.asList(createBasketStateMachine());

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
