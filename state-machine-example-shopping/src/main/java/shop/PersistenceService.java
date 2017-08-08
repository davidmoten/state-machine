package shop;

import java.io.File;
import java.io.IOException;

import javax.sql.DataSource;

import org.h2.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.stereotype.Service;

import com.github.davidmoten.fsm.persistence.Persistence;

@Service
public class PersistenceService {

	private static final Logger log = LoggerFactory.getLogger(PersistenceService.class);

	private final Persistence persistence;

	public PersistenceService() {
		DataSource dataSource = createDataSource();
		this.persistence = StateMachine.createPersistence( //
				() -> dataSource.getConnection());
		StateMachine.setup(persistence);
	}

	public Persistence get() {
		return persistence;
	}

	private static DataSource createDataSource() {
		log.info("creating data source");
		try {
			// create a new file based db in target on every startup
			File file = File.createTempFile("test", "db", new File("target"));
			return DataSourceBuilder //
					.create() //
					.url("jdbc:h2:file:" + file.getAbsolutePath()) //
					.driverClassName(Driver.class.getName()) //
					.build();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}
}
