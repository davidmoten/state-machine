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

import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
import com.github.davidmoten.fsm.persistence.Persistence;
import com.github.davidmoten.fsm.persistence.Property;

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
        p = StateMachine.createPersistence(connectionFactory);
        StateMachine.setup(p);
    }

    @RequestMapping(value = "/products", method = RequestMethod.GET)
    public List<CatalogProduct> products() {
        return p.get(CatalogProduct.class, Property.list("catalogId", "1")) //
                .stream() //
                .map(x -> x.entity) //
                .collect(Collectors.toList());
    }

}