package shop.behaviour;

import java.util.Optional;

import com.github.davidmoten.fsm.example.generated.CatalogProductBehaviourBase;
import com.github.davidmoten.fsm.example.generated.CatalogProductStateMachine;
import com.github.davidmoten.fsm.example.shop.catalogproduct.immutable.CatalogProduct;
import com.github.davidmoten.fsm.example.shop.catalogproduct.immutable.ChangeProductDetails;
import com.github.davidmoten.fsm.example.shop.catalogproduct.immutable.ChangeQuantity;
import com.github.davidmoten.fsm.example.shop.catalogproduct.immutable.Create;
import com.github.davidmoten.fsm.example.shop.product.Product;
import com.github.davidmoten.fsm.persistence.Entities;
import com.github.davidmoten.fsm.runtime.Signaller;

public final class CatalogProductBehaviour extends CatalogProductBehaviourBase<String> {

    @Override
    public CatalogProductStateMachine<String> create(String id) {
        return CatalogProductStateMachine.create(id, this);
    }

    @Override
    public CatalogProduct onEntry_Created(Signaller<CatalogProduct, String> signaller, String id, Create event,
            boolean replaying) {
        System.out.println("creating catalogproduct");
        // lookup product within the transaction
        Optional<Product> product = Entities.get().get(Product.class, event.productId());
        if (product.isPresent()) {
            return CatalogProduct.catalogId(event.catalogId()) //
                    .productId(event.productId()) //
                    .name(product.get().name) //
                    .description(product.get().description) //
                    .quantity(event.quantity()) //
                    .price(event.price()) //
                    .tags(product.get().tags);
        } else {
            throw new RuntimeException("product not found " + event.productId());
        }
    }

    @Override
    public CatalogProduct onEntry_ChangedQuantity(Signaller<CatalogProduct, String> signaller, CatalogProduct c,
            String id, ChangeQuantity event, boolean replaying) {
        System.out.println("changing quantity catalogproduct");
        // return CatalogProduct.create(c.catalogId(), c.productId(), c.name(),
        // c.description(),
        // c.quantity() + event.quantityDelta(), c.price(), c.tags());
        CatalogProduct result = c.withQuantity(c.quantity() + event.quantityDelta());
        return result;
    }

    @Override
    public CatalogProduct onEntry_ChangedProductDetails(Signaller<CatalogProduct, String> signaller, CatalogProduct c,
            String id, ChangeProductDetails event, boolean replaying) {
        return CatalogProduct.create(c.catalogId(), c.productId(), event.productName(), event.productDescription(),
                c.quantity(), c.price(), event.tags());
    }

}
