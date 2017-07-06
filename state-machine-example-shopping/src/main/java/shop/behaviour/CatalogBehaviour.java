package shop.behaviour;

import com.github.davidmoten.fsm.example.generated.CatalogBehaviourBase;
import com.github.davidmoten.fsm.example.generated.CatalogStateMachine;
import com.github.davidmoten.fsm.example.shop.catalog.Catalog;
import com.github.davidmoten.fsm.runtime.Create;
import com.github.davidmoten.fsm.runtime.Signaller;

public class CatalogBehaviour extends CatalogBehaviourBase<String> {

    @Override
    public CatalogStateMachine<String> create(String id) {
        return CatalogStateMachine.create(id, this);
    }

    @Override
    public Catalog onEntry_Created(Signaller<Catalog, String> signaller, String id, Create event, boolean replaying) {
        // TODO Auto-generated method stub
        return null;
    }

}
