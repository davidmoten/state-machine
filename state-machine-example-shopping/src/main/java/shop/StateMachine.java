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

import com.github.davidmoten.fsm.example.shop.catalog.immutable.Catalog;
import com.github.davidmoten.fsm.example.shop.catalog.immutable.Change;
import com.github.davidmoten.fsm.example.shop.catalogproduct.immutable.CatalogProduct;
import com.github.davidmoten.fsm.example.shop.product.immutable.Create;
import com.github.davidmoten.fsm.example.shop.product.immutable.Product;
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
                .of(new IntProperty(Property.combineNames("catalogId", cp.catalogId(), "price"),
                        (int) Math.floor(Math.round(cp.price().floatValue() * 100))));
        Function<CatalogProduct, Iterable<Property>> pf = c -> Property.concatenate(
                Property.list("productId", c.productId(), "catalogId", c.catalogId()), //
                Property.list("tag", c.tags()));
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
                        prod -> Property.list("tag", prod.tags())) //
                .build();
    }

    public static void setup(Persistence p) {
        try {
            p.create();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        p.initialize();
        p.signal(Catalog.class, MAIN_CATALOG_ID, com.github.davidmoten.fsm.example.shop.catalog.immutable.Create //
                .createWithCatalogId(MAIN_CATALOG_ID) //
                .name("Online bike shop"));
        InputStreamReader in = new InputStreamReader(StateMachine.class.getResourceAsStream("/products.txt"));
        try {
            for (CSVRecord record : CSVFormat.DEFAULT.parse(in)) {
                String productId = record.get(0).trim();
                String name = record.get(1).trim();
                String description = record.get(2).trim();
                List<String> tags = Arrays.asList(record.get(3).split("\\|")).stream().map(x -> x.trim())
                        .collect(Collectors.toList());
                BigDecimal price = new BigDecimal(record.get(4).trim());
                int quantity = Integer.parseInt(record.get(5).trim());
                p.signal(Product.class, //
                        productId, //
                        Create //
                                .createWithProductId(productId) //
                                .name(name) //
                                .description(description) //
                                .tags(tags));
                p.signal(Catalog.class, //
                        MAIN_CATALOG_ID, Change //
                                .createWithProductId(productId) //
                                .quantityDelta(quantity) //
                                .price(price));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
