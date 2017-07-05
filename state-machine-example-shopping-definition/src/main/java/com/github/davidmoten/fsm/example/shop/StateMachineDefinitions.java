package com.github.davidmoten.fsm.example.shop;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.github.davidmoten.fsm.example.shop.basket.Basket;
import com.github.davidmoten.fsm.example.shop.basket.event.Change;
import com.github.davidmoten.fsm.example.shop.basket.event.Checkout;
import com.github.davidmoten.fsm.example.shop.basket.event.Clear;
import com.github.davidmoten.fsm.example.shop.basket.event.Payment;
import com.github.davidmoten.fsm.example.shop.basket.event.Timeout;
import com.github.davidmoten.fsm.example.shop.catalog.Catalog;
import com.github.davidmoten.fsm.example.shop.product.Product;
import com.github.davidmoten.fsm.model.State;
import com.github.davidmoten.fsm.model.StateMachineDefinition;
import com.github.davidmoten.fsm.runtime.Create;

public final class StateMachineDefinitions implements Supplier<List<StateMachineDefinition<?>>> {

	@Override
	public List<StateMachineDefinition<?>> get() {
		return Arrays.asList(createBasketStateMachine(), //
				createCatalogStateMachine(), //
				createProductStateMachine());

	}

	private static StateMachineDefinition<Basket> createBasketStateMachine() {
		StateMachineDefinition<Basket> m = StateMachineDefinition.create(Basket.class);
		State<Basket, Create> created = m.createState("Created", Create.class).documentation("<pre>onEntry/ \n" //
				+ "send Clear to self \n" //
				+ "</pre>");
		State<Basket, Clear> empty = m.createState("Empty", Clear.class);
		State<Basket, Change> changed = m.createState("Changed", Change.class).documentation("<pre>onEntry/ \n" //
				+ "update changed items \n" //
				+ "if empty then\n" //
				+ "  send Clear to self\n" //
				+ "send Timeout to self in 1 day " //
				+ "</pre>");
		State<Basket, Checkout> checkedOut = m.createState("CheckedOut", Checkout.class).documentation("<pre>onEntry/\n" //
				+ "send Timeout to self in 1 day " //
				+ "</pre>");
		State<Basket, Payment> paid = m.createState("Paid", Payment.class).documentation("<pre>onEntry/ \n" //
				+ "create order\n" //
				+ "send order to Order \n" //
				+ "</pre>");
		State<Basket, Timeout> timedOut = m.createState("TimedOut", Timeout.class);
		created.initial() //
				.to(empty //
						.from(changed) //
						.from(timedOut //
								.from(changed) //
								.from(checkedOut))) //
				.to(changed.from(changed)) //
				.to(checkedOut) //
				.to(paid);
		return m;
	}

	private static StateMachineDefinition<Catalog> createCatalogStateMachine() {
		StateMachineDefinition<Catalog> m = StateMachineDefinition.create(Catalog.class);
		State<Catalog, Create> created = m.createState("Created", Create.class);
		State<Catalog, com.github.davidmoten.fsm.example.shop.catalog.event.Change> changed = m.createState("Changed",
				com.github.davidmoten.fsm.example.shop.catalog.event.Change.class);
		created.initial().to(changed);
		return m;
	}

	private static StateMachineDefinition<Product> createProductStateMachine() {
		StateMachineDefinition<Product> m = StateMachineDefinition.create(Product.class);
		State<Product, com.github.davidmoten.fsm.example.shop.product.event.Create> created = m.createState("Created", com.github.davidmoten.fsm.example.shop.product.event.Create.class);
		State<Product, com.github.davidmoten.fsm.example.shop.product.event.ChangeDetails> changed = m.createState("Changed", com.github.davidmoten.fsm.example.shop.product.event.ChangeDetails.class);
		created.initial().to(changed);
		return m;
	}
}
