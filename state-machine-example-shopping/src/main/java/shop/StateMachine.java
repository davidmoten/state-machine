package shop;

import java.sql.Connection;
import java.util.Collections;
import java.util.concurrent.Callable;

import com.github.davidmoten.fsm.example.shop.catalog.Catalog;
import com.github.davidmoten.fsm.example.shop.catalog.event.Change;
import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
import com.github.davidmoten.fsm.example.shop.product.Product;
import com.github.davidmoten.fsm.example.shop.product.event.ChangeDetails;
import com.github.davidmoten.fsm.example.shop.product.event.Create;
import com.github.davidmoten.fsm.persistence.Persistence;
import com.github.davidmoten.fsm.persistence.Property;
import com.github.davidmoten.guavamini.Lists;

import shop.behaviour.CatalogBehaviour;
import shop.behaviour.CatalogProductBehaviour;
import shop.behaviour.ProductBehaviour;

public final class StateMachine {

    public static Persistence createPersistence(Callable<Connection> connectionFactory) {
        return Persistence //
                .connectionFactory(connectionFactory) //
                .errorHandlerPrintStackTrace() //
                .behaviour(Product.class, new ProductBehaviour()) //
                .behaviour(Catalog.class, new CatalogBehaviour()) //
                .behaviour(CatalogProduct.class, new CatalogProductBehaviour()) //
                // set up search indexes which must exist for ProductBehaviour
                // to find stuff for instance
                .propertiesFactory(CatalogProduct.class, //
                        c -> Property.list("productId", c.productId, "catalogId", c.catalogId)) //
                .build();
    }

    public static void setup(Persistence p) {
        p.create();
        p.initialize();
        p.signal(Product.class, "12", //
                new Create("12", "Castelli Senza Jacket", "Fleece lined windproof cycling jacket",
                        Collections.emptyList()));
        p.signal(Catalog.class, "1",
                new com.github.davidmoten.fsm.example.shop.catalog.event.Create("1", "Online bike shop"));
        p.signal(Catalog.class, "1", new Change("12", 3));
        p.signal(Catalog.class, "1", new Change("12", 2));
        p.signal(Product.class, "12",
                new ChangeDetails("Castelli Senza 2 Jacket",
                        "Fleece lined windproof cycling jacket with reflective highlights",
                        Lists.newArrayList("Clothing", "Cycling", "Windproof", "Jacket", "Castelli")));
    }
}
