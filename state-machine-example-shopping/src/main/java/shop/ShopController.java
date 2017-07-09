package shop;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.github.davidmoten.fsm.example.shop.catalog.Catalog;
import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
import com.github.davidmoten.fsm.example.shop.product.Product;
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

    @RequestMapping("/catalog/{catalogId}/products")
    public String catalogProducts(@PathVariable("catalogId") String catalogId, Model model) {
        model.addAttribute("catalogProducts", persistence.get() //
                .getOr(CatalogProduct.class, Property.list("catalogId", catalogId)));
        return "catalogProducts";
    }

    @RequestMapping("/product/{productId}")
    public String products(@PathVariable("productId") String productId, Model model) {
        model.addAttribute("product", persistence.get() //
                .get(Product.class, productId).get());
        return "product";
    }

    @RequestMapping("/catalog/{catalogId}/products")
    public String catalogProductsRange(@PathVariable("catalogId") String catalogId, @RequestParam("name") String name,
            @RequestParam("vaue") String value, @RequestParam("rangeName") String rangeName,
            @RequestParam("start") int start, @RequestParam("end") int end, @RequestParam("limit") int limit,
            @RequestParam String lastId, Model model) {
        model.addAttribute("catalogProducts",
                persistence.get() //
                        .get(CatalogProduct.class, name, value, rangeName, start, true, end, false, limit,
                                Optional.ofNullable(lastId)));
        return "catalogProducts";
    }

}