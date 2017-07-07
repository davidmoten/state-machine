package shop;

import java.sql.Connection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.sql.DataSource;

import org.h2.Driver;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.github.davidmoten.fsm.example.shop.catalog.Catalog;
import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
import com.github.davidmoten.fsm.persistence.Persistence;
import com.github.davidmoten.fsm.persistence.Persistence.EntityWithId;
import com.github.davidmoten.fsm.persistence.Property;

@RestController
public class Controller {

    private final Persistence p;

    public Controller() {
        Callable<Connection> connectionFactory = () -> dataSource().getConnection();
        p = StateMachine.createPersistence(connectionFactory);
        StateMachine.setup(p);
    }

    ////////////////////////////////////////////////////////////
    // Directly exposing the internal data model is not
    // advised. The internal data model should be decoupled
    // from the REST API!
    ///////////////////////////////////////////////////////////

    @RequestMapping(value = "/catalogs/{catalogId}/products", method = RequestMethod.GET)
    public Set<EntityWithId<CatalogProduct>> products(@PathVariable("catalogId") String catalogId) {
        return p.getOr(CatalogProduct.class, Property.list("catalogId", catalogId));
    }

    @RequestMapping(value = "/products/tagged", method = RequestMethod.GET)
    public Set<EntityWithId<CatalogProduct>> productsTagged(@RequestParam("tag") List<String> tags) {
        return p.getOr(CatalogProduct.class, Property.list("tag", tags));
    }

    @RequestMapping(value = "/catalogs", method = RequestMethod.GET)
    public List<EntityWithId<Catalog>> catalogs() {
        return p.get(Catalog.class);
    }

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder //
                .create() //
                .url("jdbc:h2:mem:" + "testing") //
                .driverClassName(Driver.class.getName()) //
                .build();
    }

}