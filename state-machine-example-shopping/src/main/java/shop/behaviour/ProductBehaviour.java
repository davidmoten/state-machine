package shop.behaviour;

import java.util.Set;

import com.github.davidmoten.fsm.example.generated.ProductBehaviourBase;
import com.github.davidmoten.fsm.example.generated.ProductStateMachine;
import com.github.davidmoten.fsm.example.shop.catalogproduct.immutable.CatalogProduct;
import com.github.davidmoten.fsm.example.shop.catalogproduct.immutable.ChangeProductDetails;
import com.github.davidmoten.fsm.example.shop.product.immutable.ChangeDetails;
import com.github.davidmoten.fsm.example.shop.product.immutable.Create;
import com.github.davidmoten.fsm.example.shop.product.immutable.Product;
import com.github.davidmoten.fsm.persistence.Entities;
import com.github.davidmoten.fsm.persistence.Persistence.EntityWithId;
import com.github.davidmoten.fsm.persistence.Property;
import com.github.davidmoten.fsm.runtime.Signaller;

public final class ProductBehaviour extends ProductBehaviourBase<String> {

    @Override
    public ProductStateMachine<String> create(String id) {
        return ProductStateMachine.create(id, this);
    }

    @Override
    public Product onEntry_Created(Signaller<Product, String> signaller, String id, Create event, boolean replaying) {
        return Product.productId(event.productId()).name(event.name()).description(event.description()).tags(event.tags());
    }

    @Override
    public Product onEntry_Changed(Signaller<Product, String> signaller, Product product, String id,
            ChangeDetails event, boolean replaying) {
        // do an index-based search (using entity properties set by
        // propertiesFactory)
        Set<EntityWithId<CatalogProduct>> set = Entities.get() //
                .getOr(CatalogProduct.class, //
                        Property.list("productId", product.productId()));
        System.out.println(set);
        for (EntityWithId<CatalogProduct> cp : set) {
            signaller.signal(CatalogProduct.class, //
                    cp.id, //
                    ChangeProductDetails //
                            .productName(event.name()) //
                            .productDescription(event.description()) //
                            .tags(event.tags()));
        }
        return Product.create(product.productId(), event.name(), event.description(), event.tags());
    }
}
