package shop;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.github.davidmoten.fsm.example.shop.catalog.Catalog;
import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
import com.github.davidmoten.fsm.persistence.Persistence.EntityWithId;
import com.github.davidmoten.fsm.persistence.Property;

@RestController
public class ShopRestController {

    @Autowired
    private PersistenceService p;

    ////////////////////////////////////////////////////////////
    // Directly exposing the internal data model is not
    // advised. The internal data model should be decoupled
    // from the REST API!
    ///////////////////////////////////////////////////////////

    @RequestMapping(value = "/api/catalogs/{catalogId}/products", method = RequestMethod.GET)
    public Set<EntityWithId<CatalogProduct>> products(@PathVariable("catalogId") String catalogId) {
        return p.get().getOr(CatalogProduct.class, Property.list("catalogId", catalogId));
    }

    @RequestMapping(value = "/api/products/tagged", method = RequestMethod.GET)
    public Set<EntityWithId<CatalogProduct>> productsTagged(@RequestParam("tag") List<String> tags) {
        return p.get().getOr(CatalogProduct.class, Property.list("tag", tags));
    }

    @RequestMapping(value = "/api/catalogs", method = RequestMethod.GET)
    public List<EntityWithId<Catalog>> catalogs() {
        return p.get().get(Catalog.class);
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return new ObjectMapper() //
                .setVisibility(PropertyAccessor.FIELD, Visibility.PUBLIC_ONLY)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS) //
                .registerModule(new Jdk8Module());

    }

}