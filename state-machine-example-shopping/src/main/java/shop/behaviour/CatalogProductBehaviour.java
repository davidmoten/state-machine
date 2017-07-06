package shop.behaviour;

import com.github.davidmoten.fsm.example.generated.CatalogProductBehaviourBase;
import com.github.davidmoten.fsm.example.generated.CatalogProductStateMachine;
import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
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

}
