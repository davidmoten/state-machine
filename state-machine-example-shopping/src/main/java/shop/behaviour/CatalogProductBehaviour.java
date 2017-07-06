package shop.behaviour;

import com.github.davidmoten.fsm.example.generated.CatalogProductBehaviourBase;
import com.github.davidmoten.fsm.example.generated.CatalogProductStateMachine;
import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
import com.github.davidmoten.fsm.example.shop.catalogproduct.event.ChangeProductDetails;
import com.github.davidmoten.fsm.example.shop.catalogproduct.event.ChangeQuantity;
import com.github.davidmoten.fsm.example.shop.catalogproduct.event.Create;
import com.github.davidmoten.fsm.runtime.Signaller;

public final class CatalogProductBehaviour extends CatalogProductBehaviourBase<String> {

    @Override
    public CatalogProductStateMachine<String> create(String id) {
        return CatalogProductStateMachine.create(id, this);
    }

    @Override
    public CatalogProduct onEntry_Created(Signaller<CatalogProduct, String> signaller, String id, Create event,
            boolean replaying) {
        return new CatalogProduct(event.catalogId, event.productId, event.name, event.description, event.quantity);
    }

    @Override
    public CatalogProduct onEntry_ChangedQuantity(Signaller<CatalogProduct, String> signaller, CatalogProduct c,
            String id, ChangeQuantity event, boolean replaying) {
        return new CatalogProduct(c.catalogId, c.productId, c.name, c.description, c.quantity + event.quantityDelta);
    }

    @Override
    public CatalogProduct onEntry_ChangedProductDetails(Signaller<CatalogProduct, String> signaller, CatalogProduct c,
            String id, ChangeProductDetails event, boolean replaying) {
        return new CatalogProduct(c.catalogId, c.productId, event.productName, event.productDescription, c.quantity);
    }

}
