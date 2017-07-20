package com.github.davidmoten.fsm.example.shop;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.github.davidmoten.fsm.example.shop.basket.immutable.Basket;
import com.github.davidmoten.fsm.example.shop.basket.immutable.Change;
import com.github.davidmoten.fsm.example.shop.basket.immutable.Checkout;
import com.github.davidmoten.fsm.example.shop.basket.immutable.Clear;
import com.github.davidmoten.fsm.example.shop.basket.immutable.Payment;
import com.github.davidmoten.fsm.example.shop.basket.immutable.Timeout;
import com.github.davidmoten.fsm.example.shop.catalog.immutable.Catalog;
import com.github.davidmoten.fsm.example.shop.catalogproduct.immutable.CatalogProduct;
import com.github.davidmoten.fsm.example.shop.product.immutable.Product;
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

    private static StateMachineDefinition<Basket> createBasketStateMachine() {
        StateMachineDefinition<Basket> m = StateMachineDefinition.create(Basket.class);
        State<Basket, Create> created = m.createState("Created", Create.class)
                .documentation("<pre>onEntry/ \n" //
                        + "send Clear to self \n" //
                        + "</pre>");
        State<Basket, Clear> empty = m.createState("Empty", Clear.class);
        State<Basket, Change> changed = m.createState("Changed", Change.class)
                .documentation("<pre>onEntry/ \n" //
                        + "update changed items \n" //
                        + "if empty then\n" //
                        + "  send Clear to self\n" //
                        + "send Timeout to self in 1 day " //
                        + "</pre>");
        State<Basket, Checkout> checkedOut = m.createState("CheckedOut", Checkout.class)
                .documentation("<pre>onEntry/\n" //
                        + "send Timeout to self in 1 day " //
                        + "</pre>");
        State<Basket, Payment> paid = m.createState("Paid", Payment.class)
                .documentation("<pre>onEntry/ \n" //
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
        State<Catalog, com.github.davidmoten.fsm.example.shop.catalog.immutable.Create> created = m
                .createState("Created", com.github.davidmoten.fsm.example.shop.catalog.immutable.Create.class);
        State<Catalog, com.github.davidmoten.fsm.example.shop.catalog.immutable.Change> changed = m
                .createState("Changed", com.github.davidmoten.fsm.example.shop.catalog.immutable.Change.class);
        created.initial().to(changed).to(changed);
        return m;
    }

    private static StateMachineDefinition<Product> createProductStateMachine() {
        StateMachineDefinition<Product> m = StateMachineDefinition.create(Product.class);
        State<Product, com.github.davidmoten.fsm.example.shop.product.immutable.Create> created = m
                .createState("Created", com.github.davidmoten.fsm.example.shop.product.immutable.Create.class);
        State<Product, com.github.davidmoten.fsm.example.shop.product.immutable.ChangeDetails> changed = m
                .createState("Changed", com.github.davidmoten.fsm.example.shop.product.immutable.ChangeDetails.class);
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
