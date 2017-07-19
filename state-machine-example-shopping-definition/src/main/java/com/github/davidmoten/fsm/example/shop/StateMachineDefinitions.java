package com.github.davidmoten.fsm.example.shop;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.github.davidmoten.fsm.example.shop.basket.event.Change;
import com.github.davidmoten.fsm.example.shop.basket.event.Checkout;
import com.github.davidmoten.fsm.example.shop.basket.event.Clear;
import com.github.davidmoten.fsm.example.shop.basket.event.Payment;
import com.github.davidmoten.fsm.example.shop.basket.event.Timeout;
import com.github.davidmoten.fsm.example.shop.catalog.immutable.Catalog;
import com.github.davidmoten.fsm.example.shop.catalogproduct.immutable.CatalogProduct;
import com.github.davidmoten.fsm.example.shop.customer.Customer;
import com.github.davidmoten.fsm.example.shop.product.Product;
import com.github.davidmoten.fsm.model.State;
import com.github.davidmoten.fsm.model.StateMachineDefinition;
import com.github.davidmoten.fsm.runtime.Create;

public final class StateMachineDefinitions implements Supplier<List<StateMachineDefinition<?>>> {

    @Override
    public List<StateMachineDefinition<?>> get() {
        return Arrays.asList(createBasketStateMachine(), //
                createCatalogStateMachine(), //
                createProductStateMachine(), //
                createCatalogProductStateMachine());

    }

    private static StateMachineDefinition<Customer> createBasketStateMachine() {
        StateMachineDefinition<Customer> m = StateMachineDefinition.create(Customer.class);
        State<Customer, Create> created = m.createState("Created", Create.class).documentation("<pre>onEntry/ \n" //
                + "send Clear to self \n" //
                + "</pre>");
        State<Customer, Clear> empty = m.createState("Empty", Clear.class);
        State<Customer, Change> changed = m.createState("Changed", Change.class).documentation("<pre>onEntry/ \n" //
                + "update changed items \n" //
                + "if empty then\n" //
                + "  send Clear to self\n" //
                + "send Timeout to self in 1 day " //
                + "</pre>");
        State<Customer, Checkout> checkedOut = m.createState("CheckedOut", Checkout.class)
                .documentation("<pre>onEntry/\n" //
                        + "send Timeout to self in 1 day " //
                        + "</pre>");
        State<Customer, Payment> paid = m.createState("Paid", Payment.class).documentation("<pre>onEntry/ \n" //
                + "create order\n" //
                + "send order to Order \n" //
                + "</pre>");
        State<Customer, Timeout> timedOut = m.createState("TimedOut", Timeout.class);
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
        State<Catalog, com.github.davidmoten.fsm.example.shop.catalog.immutable.Create> created = m.createState("Created",
                com.github.davidmoten.fsm.example.shop.catalog.immutable.Create.class);
        State<Catalog, com.github.davidmoten.fsm.example.shop.catalog.immutable.Change> changed = m.createState("Changed",
                com.github.davidmoten.fsm.example.shop.catalog.immutable.Change.class);
        created.initial().to(changed).to(changed);
        return m;
    }

    private static StateMachineDefinition<Product> createProductStateMachine() {
        StateMachineDefinition<Product> m = StateMachineDefinition.create(Product.class);
        State<Product, com.github.davidmoten.fsm.example.shop.product.event.Create> created = m.createState("Created",
                com.github.davidmoten.fsm.example.shop.product.event.Create.class);
        State<Product, com.github.davidmoten.fsm.example.shop.product.event.ChangeDetails> changed = m
                .createState("Changed", com.github.davidmoten.fsm.example.shop.product.event.ChangeDetails.class);
        created.initial().to(changed).to(changed);
        return m;
    }

    private static StateMachineDefinition<CatalogProduct> createCatalogProductStateMachine() {
        StateMachineDefinition<CatalogProduct> m = StateMachineDefinition.create(CatalogProduct.class);
        State<CatalogProduct, com.github.davidmoten.fsm.example.shop.catalogproduct.immutable.Create> created = m
                .createState("Created", com.github.davidmoten.fsm.example.shop.catalogproduct.immutable.Create.class);
        State<CatalogProduct, com.github.davidmoten.fsm.example.shop.catalogproduct.immutable.ChangeQuantity> changedQuantity = m
                .createState("ChangedQuantity",
                        com.github.davidmoten.fsm.example.shop.catalogproduct.immutable.ChangeQuantity.class);
        State<CatalogProduct, com.github.davidmoten.fsm.example.shop.catalogproduct.immutable.ChangeProductDetails> changedProductDetails = m
                .createState("ChangedProductDetails",
                        com.github.davidmoten.fsm.example.shop.catalogproduct.immutable.ChangeProductDetails.class);
        created.initial() //
                .to(changedQuantity) //
                .to(changedQuantity) //
                .to(changedProductDetails) //
                .to(changedProductDetails.from(created));

        return m;
    }
}
