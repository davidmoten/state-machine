package shop;

import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import com.github.davidmoten.fsm.example.shop.catalog.Catalog;
import com.github.davidmoten.fsm.example.shop.catalog.event.Change;
import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
import com.github.davidmoten.fsm.example.shop.product.Product;
import com.github.davidmoten.fsm.example.shop.product.event.Create;
import com.github.davidmoten.fsm.persistence.Persistence;
import com.github.davidmoten.fsm.persistence.Property;

import shop.behaviour.CatalogBehaviour;
import shop.behaviour.CatalogProductBehaviour;
import shop.behaviour.ProductBehaviour;

public final class StateMachine {

    private static final String MAIN_CATALOG_ID = "1";

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
        p.signal(Catalog.class, MAIN_CATALOG_ID,
                new com.github.davidmoten.fsm.example.shop.catalog.event.Create(MAIN_CATALOG_ID, "Online bike shop"));
        p.signal(Catalog.class, MAIN_CATALOG_ID, new Change("12", 3));
        p.signal(Catalog.class, MAIN_CATALOG_ID, new Change("12", 2));
        InputStreamReader in = new InputStreamReader(StateMachine.class.getResourceAsStream("/products.txt"));
        try {
            for (CSVRecord record : CSVFormat.DEFAULT.parse(in)) {
                String productId = record.get(0).trim();
                String name = record.get(1).trim();
                String description = record.get(2).trim();
                List<String> tags = Arrays.asList(record.get(3).split("\\|")).stream().map(x -> x.trim())
                        .collect(Collectors.toList());
                int quantity = Integer.parseInt(record.get(4).trim());
                p.signal(Product.class, //
                        productId, //
                        new Create(productId, name, description, tags));
                p.signal(Catalog.class, //
                        MAIN_CATALOG_ID, new Change(productId, quantity));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // p.signal(Product.class, "12",
        // new ChangeDetails("Castelli Senza 2 Jacket",
        // "Fleece lined windproof cycling jacket with reflective highlights",
        // Lists.newArrayList("Clothing", "Cycling", "Windproof", "Jacket",
        // "Castelli")));
    }

}
