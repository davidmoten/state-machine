package shop;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.junit.Assert;
import org.junit.Test;

import com.github.davidmoten.fsm.example.shop.catalog.Catalog;
import com.github.davidmoten.fsm.example.shop.catalog.event.Change;
import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
import com.github.davidmoten.fsm.example.shop.product.Product;
import com.github.davidmoten.fsm.example.shop.product.event.ChangeDetails;
import com.github.davidmoten.fsm.example.shop.product.event.Create;
import com.github.davidmoten.fsm.persistence.Persistence;
import com.github.davidmoten.fsm.persistence.Property;
import com.github.davidmoten.fsm.persistence.Serializer;

import shop.behaviour.CatalogBehaviour;
import shop.behaviour.CatalogProductBehaviour;
import shop.behaviour.ProductBehaviour;

public class ShopTest {

    @Test
    public void testSerializeProduct() {
        Product p = new Product("a", "name", "description");
        byte[] s = Serializer.JSON.serialize(p);
        Serializer.JSON.deserialize(Product.class, s);
    }

    @Test
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
                .properties(CatalogProduct.class, //
                        c -> Property.list("productId", c.productId, "catalogId", c.catalogId)) //
                .build();
        p.create();
        p.initialize();
        p.signal(Product.class, "12", //
                new Create("1", "Castelli Senza Jacket", "Fleece lined windproof cycling jacket"));
        p.signal(Catalog.class, "1",
                new com.github.davidmoten.fsm.example.shop.catalog.event.Create("1", "Online bike shop"));
        p.signal(Catalog.class, "1", new Change("12", 3));
        p.signal(Catalog.class, "1", new Change("12", 2));
        Thread.sleep(500);
        {
            CatalogProduct cp = p.get(CatalogProduct.class, CatalogProduct.idFrom("1", "12")).get();
            assertEquals("Castelli Senza Jacket", cp.name);
            assertEquals(5, cp.quantity);
        }
        p.signal(Product.class, "12", new ChangeDetails("Castelli Senza 2 Jacket",
                "Fleece lined windproof cycling jacket with reflective highlights"));
        Thread.sleep(500);
        {
            CatalogProduct cp = p.get(CatalogProduct.class, CatalogProduct.idFrom("1", "12")).get();
            assertEquals("Castelli Senza 2 Jacket", cp.name);
            assertEquals(5, cp.quantity);
        }

    }

}
