package shop.behaviour;

import com.github.davidmoten.fsm.example.generated.CatalogBehaviourBase;
import com.github.davidmoten.fsm.example.generated.CatalogStateMachine;
import com.github.davidmoten.fsm.example.shop.catalog.immutable.Catalog;
import com.github.davidmoten.fsm.example.shop.catalog.immutable.Change;
import com.github.davidmoten.fsm.example.shop.catalog.immutable.Create;
import com.github.davidmoten.fsm.example.shop.catalogproduct.immutable.CatalogProduct;
import com.github.davidmoten.fsm.example.shop.catalogproduct.immutable.ChangeQuantity;
import com.github.davidmoten.fsm.runtime.Signaller;

public final class CatalogBehaviour extends CatalogBehaviourBase<String> {

    @Override
    public CatalogStateMachine<String> create(String id) {
        return CatalogStateMachine.create(id, this);
    }

    @Override
    public Catalog onEntry_Created(Signaller<Catalog, String> signaller, String id, Create event, boolean replaying) {
        return Catalog //
                .createWithCatalogId(event.catalogId()) //
                .name(event.name());
    }

    @Override
    public Catalog onEntry_Changed(Signaller<Catalog, String> signaller, Catalog catalog, String id, Change event,
            boolean replaying) {
        System.out.println("catalog changed quantity " + event.quantityDelta());
        String cpId = CatalogProduct.idFrom(catalog.catalogId(), event.productId());
        signaller.signal(CatalogProduct.class, cpId,
                com.github.davidmoten.fsm.example.shop.catalogproduct.immutable.Create //
                        .createWithCatalogId(id) //
                        .productId(event.productId()) //
                        .quantity(0) //
                        .price(event.price()));
        signaller.signal(CatalogProduct.class, cpId, ChangeQuantity //
                .createWithQuantityDelta(event.quantityDelta()));
        return catalog;
    }

}
