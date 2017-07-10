package shop;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import com.github.davidmoten.fsm.example.shop.catalog.Catalog;
import com.github.davidmoten.fsm.example.shop.catalog.event.Change;
import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
import com.github.davidmoten.fsm.example.shop.product.Product;
import com.github.davidmoten.fsm.example.shop.product.event.Create;
import com.github.davidmoten.fsm.persistence.IntProperty;
import com.github.davidmoten.fsm.persistence.Persistence;
import com.github.davidmoten.fsm.persistence.Property;

import shop.behaviour.CatalogBehaviour;
import shop.behaviour.CatalogProductBehaviour;
import shop.behaviour.ProductBehaviour;

public final class StateMachine {

    private static final String MAIN_CATALOG_ID = "1";

    @SuppressWarnings("unchecked")
    public static Persistence createPersistence(Callable<Connection> connectionFactory) {
        Function<CatalogProduct, Optional<IntProperty>> rf = cp -> Optional
                .of(new IntProperty(Property.combineNames("catalogId", cp.catalogId, "price"),
                        (int) Math.floor(Math.round(cp.price.floatValue() * 100))));
        Function<CatalogProduct, Iterable<Property>> pf = c -> Property.concatenate(
                Property.list("productId", c.productId, "catalogId", c.catalogId), //
                Property.list("tag", c.tags));
        return Persistence //
                .connectionFactory(connectionFactory) //
                .errorHandlerPrintStackTrace() //
                .behaviour(Product.class, new ProductBehaviour()) //
                .behaviour(Catalog.class, new CatalogBehaviour()) //
                .behaviour(CatalogProduct.class, new CatalogProductBehaviour()) //
                // set up search indexes which must exist for ProductBehaviour
                // to find stuff for instance
                .propertiesFactory(CatalogProduct.class, pf) //
                .rangeMetricFactory(CatalogProduct.class, rf) //
                .propertiesFactory(Product.class, //
                        prod -> Property.list("tag", prod.tags)) //
                .build();
    }

    public static void setup(Persistence p) {
        try {
            p.create();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        p.initialize();
        p.signal(Catalog.class, MAIN_CATALOG_ID,
                new com.github.davidmoten.fsm.example.shop.catalog.event.Create(MAIN_CATALOG_ID, "Online bike shop"));
        p.signal(Catalog.class, MAIN_CATALOG_ID, new Change("12", new BigDecimal("142.50"), 3));
        p.signal(Catalog.class, MAIN_CATALOG_ID, new Change("12", new BigDecimal("142.50"), 2));
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
                        MAIN_CATALOG_ID, new Change(productId, new BigDecimal("144.70"), quantity));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
