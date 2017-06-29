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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import com.github.davidmoten.fsm.runtime.EntityStateMachine;
import com.github.davidmoten.fsm.runtime.Event;
import com.github.davidmoten.fsm.runtime.Signal;

public final class PersistenceH2<T> implements Persistence<T> {

    private final File directory;
    private ExecutorService executor;

    public PersistenceH2(File directory, ExecutorService executor) {
        this.directory = directory;
        this.executor = executor;
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
        }
    }

    public void initialize() {
        try (Connection con = createConnection()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "select cls, id, event_cls, event_bytes, time from delayed_signal_queue order by seq_num")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String className = rs.getString(1);
                        String id = rs.getString(2);
                        String eventCls = rs.getString(3);
                        byte[] eventBytes = readAll(rs.getBlob(4).getBinaryStream());
                        long time = rs.getTimestamp(5).getTime();
                    }
                }
            }
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private static byte[] readAll(InputStream is) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        try {
            while ((count = is.read(buffer)) != -1) {
                bytes.write(buffer, 0, count);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bytes.toByteArray();
    }

    @Override
    public EntityStateMachine<T, String> process(EntityStateMachine<T, String> esm,
            Serializer serializer, Event<T> event, Serializer eventSerializer) {
        try (Connection con = createConnection()) {
            con.setAutoCommit(false);
            insertIntoSignalStore(esm, event, eventSerializer, con);
            EntityStateMachine<T, String> esm2 = esm.signal(event);
            List<Signal<?, ?>> signalsToOther = esm2.signalsToOther();
            insertSignalsToOther(eventSerializer, con, signalsToOther);
            insertDelayedSignalsToOther(eventSerializer, con, signalsToOther);
            return esm2;
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private void insertDelayedSignalsToOther(Serializer eventSerializer, Connection con,
            List<Signal<?, ?>> signalsToOther) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "insert into delayed_signal_queue(cls, id, event_cls, event_bytes, time) values(?,?,?,?,?)")) {
            for (Signal<?, ?> signal : signalsToOther) {
                if (signal.time().isPresent()) {
                    @SuppressWarnings("unchecked")
                    Signal<?, String> sig = (Signal<?, String>) signal;
                    ps.setString(1, sig.cls().getName());
                    ps.setString(2, sig.id());
                    ps.setString(3, sig.event().getClass().getName());
                    ps.setBlob(4, new ByteArrayInputStream(eventSerializer.serialize(sig.event())));
                    ps.setTimestamp(5, new Timestamp(sig.time().get()));
                    ps.executeUpdate();
                }
            }
        }
    }

    private void insertSignalsToOther(Serializer eventSerializer, Connection con,
            List<Signal<?, ?>> signalsToOther) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "insert into signal_queue(cls, id, event_cls, event_bytes) values(?,?,?,?)")) {
            for (Signal<?, ?> signal : signalsToOther) {
                if (!signal.time().isPresent()) {
                    @SuppressWarnings("unchecked")
                    Signal<?, String> sig = (Signal<?, String>) signal;
                    ps.setString(1, sig.cls().getName());
                    ps.setString(2, sig.id());
                    ps.setString(3, sig.event().getClass().getName());
                    ps.setBlob(4, new ByteArrayInputStream(eventSerializer.serialize(sig.event())));
                    ps.executeUpdate();
                }
            }
        }
    }

    private void insertIntoSignalStore(EntityStateMachine<T, String> esm, Event<T> event,
            Serializer eventSerializer, Connection con) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "insert into signal_store(cls, id, event_cls, event_bytes) values(?,?,?,?)")) {
            ps.setString(1, esm.cls().getName());
            ps.setString(2, esm.id());
            ps.setString(3, event.getClass().getName());
            ps.setBlob(4, new ByteArrayInputStream(eventSerializer.serialize(event)));
            ps.executeUpdate();
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
