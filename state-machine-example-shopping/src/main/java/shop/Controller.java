package shop;

import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.h2.Driver;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.github.davidmoten.fsm.example.shop.product.Product;
import com.github.davidmoten.fsm.example.shop.product.event.Create;
import com.github.davidmoten.fsm.persistence.Persistence;

@RestController
public class Controller {

	private final Persistence p;

	@Bean
	public DataSource dataSource() {
		File db = new File("target/db");
		return DataSourceBuilder //
				.create() //
				.url("jdbc:h2:"+ db.getAbsolutePath()) //
				.driverClassName(Driver.class.getName()) //
				.build();
	}

	public Controller() {
		File file = new File("target/db");
		boolean dbExists = file.exists();
		p = Persistence //
				.connectionFactory(() -> dataSource().getConnection()) //
				.errorHandlerPrintStackTraceAndThrow() //
				.behaviourFactory(cls -> {
					if (Product.class.getName().equals(cls.getName())) {
						return new ProductBehaviour();
					} else {
						throw new RuntimeException("behaviour not defined for " + cls);
					}
				}) //
				.build();
		try {
			DriverManager.registerDriver(Driver.load());
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
		if (!dbExists) {
			p.create();
		}
		p.initialize();
		p.signal(Product.class, "1", new Create("1", "Castelli Senza Jacket", "Fleece lined windproof cycling jacket"));
	}

	@RequestMapping(value = "/products", method = RequestMethod.GET)
	public List<Product> products() {
		return p.get(Product.class).stream().map(x -> x.entity).collect(Collectors.toList());
	}

}