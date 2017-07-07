package shop;

import javax.sql.DataSource;

import org.h2.Driver;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.github.davidmoten.fsm.persistence.Persistence;

@Service
public class PersistenceService {

    private final Persistence persistence;

    public PersistenceService() {
        this.persistence = StateMachine.createPersistence( //
                () -> dataSource().getConnection());
        StateMachine.setup(persistence);
    }

    public Persistence get() {
        return persistence;
    }

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder //
                .create() //
                .url("jdbc:h2:mem:" + "testing") //
                .driverClassName(Driver.class.getName()) //
                .build();
    }
}
