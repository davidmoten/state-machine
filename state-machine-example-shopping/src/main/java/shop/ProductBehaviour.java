package shop;

import com.github.davidmoten.fsm.example.generated.ProductBehaviourBase;
import com.github.davidmoten.fsm.example.generated.ProductStateMachine;
import com.github.davidmoten.fsm.example.shop.product.Product;
import com.github.davidmoten.fsm.example.shop.product.event.Create;
import com.github.davidmoten.fsm.runtime.Signaller;

public class ProductBehaviour extends ProductBehaviourBase<String> {

	@Override
	public ProductStateMachine<String> create(String id) {
		return ProductStateMachine.create(id, this);
	}

	@Override
	public Product onEntry_Created(Signaller<Product, String> signaller, String id, Create event, boolean replaying) {
		return new Product(event.productId, event.name, event.description);
	}


}
