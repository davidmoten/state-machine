package com.github.davidmoten.fsm.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

import com.github.davidmoten.fsm.runtime.EntityStateMachine;
import com.github.davidmoten.fsm.runtime.Event;
import com.github.davidmoten.fsm.runtime.Signal;

public final class PersistenceH2<T> implements Persistence<T> {

    private final File directory;

    public PersistenceH2(File directory) {
        this.directory = directory;
    }

    public void create() {
        directory.mkdirs();
        try (Connection con = createConnection()) {
            con.setAutoCommit(true);
            String sql = new String(
                    readAll(PersistenceH2.class.getResourceAsStream("/create-h2.sql")),
                    StandardCharsets.UTF_8);
            String[] commands = sql.split(";");
            for (String command : commands) {
                con.prepareStatement(command).execute();
            }
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = is.read(buffer)) != -1) {
            bytes.write(buffer, 0, count);
        }
        return bytes.toByteArray();
    }

    @Override
    public EntityStateMachine<T, String> process(EntityStateMachine<T, String> esm,
            Serializer serializer, Event<T> event, Serializer eventSerializer) {
        try (Connection con = createConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(
                    "insert into signal_store(cls, id, event_cls, event_bytes) values(?,?,?,?)")) {
                ps.setString(1, esm.cls().getName());
                ps.setString(2, esm.id());
                ps.setString(3, event.getClass().getName());
                ps.setBlob(4, new ByteArrayInputStream(eventSerializer.serialize(event)));
                ps.executeUpdate();
            }
            EntityStateMachine<T, String> esm2 = esm.signal(event);
            try (PreparedStatement ps = con.prepareStatement(
                    "insert into signal_queue(cls, id, event_cls, event_bytes) values(?,?,?,?")) {
                for (Signal<?, ?> signal : esm2.signalsToOther()) {
                    @SuppressWarnings("unchecked")
                    Signal<?, String> sig = (Signal<?, String>) signal;
                    ps.setString(1, sig.cls().getName());
                    ps.setString(2, sig.id());
                    ps.setString(3, sig.event().getClass().getName());
                    ps.setBlob(4, new ByteArrayInputStream(eventSerializer.serialize(sig.event())));
                    ps.executeUpdate();
                }
            }
            return esm2;
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    @Override
    public EntityStateMachine<T, String> replay(Class<T> cls, String id) {
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

    @Override
    public Optional<T> get(Class<T> cls, String id) {
        // TODO Auto-generated method stub
        return null;
    }

}
