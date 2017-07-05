package shop;

import org.junit.Test;

import com.github.davidmoten.fsm.example.shop.product.Product;
import com.github.davidmoten.fsm.persistence.Serializer;

public class ShopTest {
	
	@Test
	public void testSerializeProduct() {
		Product p = new Product("a", "name", "description");
		byte[] s = Serializer.JSON.serialize(p);
		Serializer.JSON.deserialize(Product.class, s);
	}

}
