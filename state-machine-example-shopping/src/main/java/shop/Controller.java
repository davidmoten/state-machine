package shop;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
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
import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
import com.github.davidmoten.fsm.example.shop.product.Product;
import com.github.davidmoten.fsm.example.shop.product.event.Create;
import com.github.davidmoten.fsm.persistence.Persistence;
import com.github.davidmoten.fsm.persistence.Property;

import shop.behaviour.CatalogBehaviour;
import shop.behaviour.ProductBehaviour;

@RestController
public class Controller {

    private final Persistence p;

    @Bean
    public DataSource dataSource() {
        File db = new File("target/db" + UUID.randomUUID().toString().substring(0, 8));
        return DataSourceBuilder //
                .create() //
                .url("jdbc:h2:" + db.getAbsolutePath()) //
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
                .properties(CatalogProduct.class, //
                        c -> Property.list("productId", c.productId)) //
                .build();
        // try {
        // DriverManager.registerDriver(Driver.load());
        // } catch (SQLException e) {
        // throw new RuntimeException(e);
        // }
        p.create();
        p.initialize();
        p.signal(Product.class, "1", //
                new Create("1", "Castelli Senza Jacket", "Fleece lined windproof cycling jacket"));
        p.signal(Catalog.class, "1",
                new com.github.davidmoten.fsm.example.shop.catalog.event.Create("1", "Online bike shop"));
    }

    @RequestMapping(value = "/products", method = RequestMethod.GET)
    public List<Product> products() {
        return p.get(Product.class) //
                .stream() //
                .map(x -> x.entity) //
                .collect(Collectors.toList());
    }

}