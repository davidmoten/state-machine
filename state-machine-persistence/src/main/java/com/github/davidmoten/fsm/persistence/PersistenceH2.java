package com.github.davidmoten.fsm.persistence;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.github.davidmoten.fsm.runtime.EntityStateMachine;
import com.github.davidmoten.fsm.runtime.Signal;
import com.github.davidmoten.fsm.runtime.rx.ClassId;

public final class PersistenceH2<T> implements Persistence<T> {

    private final File directory;

    public PersistenceH2(File directory) {
        this.directory = directory;
    }

    @Override
    public EntityStateMachine<T, String> process(EntityStateMachine<T, String> esm, Serializer<? super T> serializer,
            Signal<T, String> signal) {
        try (Connection con = createConnection()) {
            con.setAutoCommit(false);
            // add signals to others to Signal table
            // set state in Entity table
            // set bytes in Entity table
            // add signal.event to EntityEvent table
            return null;
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    @Override
    public EntityStateMachine<T, String> replay(ClassId<T, String> classId) {
        try (Connection con = createConnection()) {
            return null;
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private Connection createConnection() {
        try {
            return DriverManager.getConnection("jdbc:h2:" + directory.getAbsolutePath());
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

}
