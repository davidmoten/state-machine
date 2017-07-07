package shop;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.github.davidmoten.fsm.example.shop.catalog.Catalog;
import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
import com.github.davidmoten.fsm.persistence.Property;

@Controller
public class ShopController {

    @Autowired
    private PersistenceService persistence;

    @RequestMapping("/catalogs")
    public String catalog(Model model) {
        model.addAttribute("catalogs", persistence.get().get(Catalog.class));
        return "catalogs";
    }

    @RequestMapping("/catalogs/{catalogId}/products")
    public String catalogProducts(@PathVariable("catalogId") String catalogId, Model model) {
        model.addAttribute("catalogProducts", persistence.get() //
                .getOr(CatalogProduct.class, Property.list("catalogId", catalogId)));
        return "catalogProducts";
    }

}