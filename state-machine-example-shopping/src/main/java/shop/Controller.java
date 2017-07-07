package shop;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.h2.Driver;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.github.davidmoten.fsm.example.shop.catalog.Catalog;
import com.github.davidmoten.fsm.example.shop.catalog.event.Change;
import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
import com.github.davidmoten.fsm.example.shop.product.Product;
import com.github.davidmoten.fsm.example.shop.product.event.ChangeDetails;
import com.github.davidmoten.fsm.example.shop.product.event.Create;
import com.github.davidmoten.fsm.persistence.Persistence;
import com.github.davidmoten.fsm.persistence.Property;

import shop.behaviour.CatalogBehaviour;
import shop.behaviour.CatalogProductBehaviour;
import shop.behaviour.ProductBehaviour;

@RestController
public class Controller {

    private final Persistence p;

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder //
                .create() //
                .url("jdbc:h2:mem:" + "testing") //
                .driverClassName(Driver.class.getName()) //
                .build();
    }

    public Controller() {
        Callable<Connection> connectionFactory = () -> dataSource().getConnection();
        p = Persistence //
                .connectionFactory(connectionFactory) //
                .errorHandlerPrintStackTraceAndThrow() //
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
        p.signal(Product.class, "12", new ChangeDetails("Castelli Senza 2 Jacket",
                "Fleece lined windproof cycling jacket with reflective highlights"));
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        CatalogProduct cp = p.get(CatalogProduct.class, CatalogProduct.idFrom("1", "12")).get();
        assert cp.name.equals("Castelli Senza 2 Jacket");
        assert cp.quantity == 5;
    }

    @RequestMapping(value = "/products", method = RequestMethod.GET)
    public List<CatalogProduct> products() {
        return p.get(CatalogProduct.class, Property.list("catalogId", "1")) //
                .stream() //
                .map(x -> x.entity) //
                .collect(Collectors.toList());
    }

}