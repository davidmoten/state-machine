package shop;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;

import org.junit.Test;

import com.github.davidmoten.fsm.example.shop.catalog.immutable.Catalog;
import com.github.davidmoten.fsm.example.shop.catalog.immutable.Change;
import com.github.davidmoten.fsm.example.shop.catalogproduct.immutable.CatalogProduct;
import com.github.davidmoten.fsm.example.shop.product.Product;
import com.github.davidmoten.fsm.example.shop.product.event.ChangeDetails;
import com.github.davidmoten.fsm.example.shop.product.event.Create;
import com.github.davidmoten.fsm.persistence.Persistence;
import com.github.davidmoten.fsm.persistence.Persistence.EntityWithId;
import com.github.davidmoten.fsm.persistence.Property;
import com.github.davidmoten.fsm.persistence.Serializer;
import com.github.davidmoten.guavamini.Lists;

import shop.behaviour.CatalogBehaviour;
import shop.behaviour.CatalogProductBehaviour;
import shop.behaviour.ProductBehaviour;

public class ShopTest {

    @Test
    public void testSerializeProduct() {
        Product p = new Product("a", "name", "description", Lists.newArrayList());
        byte[] s = Serializer.JSON.serialize(p);
        Serializer.JSON.deserialize(Product.class, s);
    }

    @SuppressWarnings("unchecked")
    @Test(timeout = 30000)
    public void testPersistence() throws IOException, InterruptedException {
        File file = File.createTempFile("db-test", "", new File("target"));
        Callable<Connection> connectionFactory = //
                () -> DriverManager.getConnection("jdbc:h2:" + file.getAbsolutePath());
        Persistence p = Persistence //
                .connectionFactory(connectionFactory) //
                .errorHandlerPrintStackTrace() //
                .behaviour(Product.class, new ProductBehaviour()) //
                .behaviour(Catalog.class, new CatalogBehaviour()) //
                .behaviour(CatalogProduct.class, new CatalogProductBehaviour()) //
                .propertiesFactory(CatalogProduct.class, //
                        c -> Property.concatenate(Property.list("productId", c.productId(), "catalogId", c.catalogId()), //
                                Property.list("tag", c.tags()))) //
                .propertiesFactory(Product.class, //
                        prod -> Property.list("tag", prod.tags)) //
                .build();
        p.create();
        p.initialize();
        p.signal(Product.class, "12", //
                new Create("12", "Castelli Senza Jacket", "Fleece lined windproof cycling jacket",
                        Collections.emptyList()));
        p.signal(Catalog.class, "1",
                com.github.davidmoten.fsm.example.shop.catalog.immutable.Create.create("1", "Online bike shop"));
        p.signal(Catalog.class, "1", Change.create("12", 3, new BigDecimal(141.30)));
        p.signal(Catalog.class, "1", Change.create("12", 2, new BigDecimal(151.75)));

        while (true) {
            Thread.sleep(100);
            Optional<CatalogProduct> cp = p.get(CatalogProduct.class, CatalogProduct.idFrom("1", "12"));
            if (cp.isPresent()) {
                assertEquals("Castelli Senza Jacket", cp.get().name());
                if (cp.get().quantity() == 5) {
                    break;
                }
            }
        }
        p.signal(Product.class, "12",
                new ChangeDetails("Castelli Senza 2 Jacket",
                        "Fleece lined windproof cycling jacket with reflective highlights",
                        Lists.newArrayList("Clothing", "Cycling", "Windproof", "Jacket", "Castelli")));
        while (true) {
            Optional<CatalogProduct> cp = p.get(CatalogProduct.class, CatalogProduct.idFrom("1", "12"));
            if (cp.get().name().equals("Castelli Senza 2 Jacket")) {
                assertEquals(5, cp.get().quantity());
                break;
            }

        }
        {
            Set<EntityWithId<Product>> list = p.get(Product.class, "tag", "Clothing");
            assertEquals(1, list.size());
        }

    }

}
