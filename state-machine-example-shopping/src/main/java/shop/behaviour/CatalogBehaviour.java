package shop.behaviour;

import com.github.davidmoten.fsm.example.generated.CatalogBehaviourBase;
import com.github.davidmoten.fsm.example.generated.CatalogStateMachine;
import com.github.davidmoten.fsm.example.shop.catalog.Catalog;
import com.github.davidmoten.fsm.example.shop.catalog.event.Change;
import com.github.davidmoten.fsm.example.shop.catalog.event.Create;
import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
import com.github.davidmoten.fsm.example.shop.catalogproduct.event.ChangeQuantity;
import com.github.davidmoten.fsm.runtime.Signaller;

public final class CatalogBehaviour extends CatalogBehaviourBase<String> {

    @Override
    public CatalogStateMachine<String> create(String id) {
        return CatalogStateMachine.create(id, this);
    }

    @Override
    public Catalog onEntry_Created(Signaller<Catalog, String> signaller, String id, Create event, boolean replaying) {
        return new Catalog(event.catalogId, event.name);
    }

    @Override
    public Catalog onEntry_Changed(Signaller<Catalog, String> signaller, Catalog catalog, String id, Change event,
            boolean replaying) {
        String cpId = CatalogProduct.idFrom(catalog.catalogId, event.productId);
        signaller.signal(CatalogProduct.class, cpId,
                new com.github.davidmoten.fsm.example.shop.catalogproduct.event.Create(id, event.productId, 0));
        signaller.signal(CatalogProduct.class, cpId, new ChangeQuantity(event.quantityDelta));
        return catalog;
    }

}
