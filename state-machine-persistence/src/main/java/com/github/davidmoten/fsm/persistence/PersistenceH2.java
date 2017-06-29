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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.davidmoten.fsm.runtime.Clock;
import com.github.davidmoten.fsm.runtime.EntityStateMachine;
import com.github.davidmoten.fsm.runtime.Event;
import com.github.davidmoten.fsm.runtime.Signal;

public final class PersistenceH2 implements Persistence {

    private final File directory;
    private final ScheduledExecutorService executor;
    private final Clock clock;
    private final Serializer entitySerializer;
    private final Serializer eventSerializer;
    private final Queue<Signal<?, ?>> queue = new LinkedList<>();
    private final AtomicInteger wip = new AtomicInteger();

    public PersistenceH2(File directory, ScheduledExecutorService executor, Clock clock,
            Serializer entitySerializer, Serializer eventSerializer) {
        this.directory = directory;
        this.executor = executor;
        this.clock = clock;
        this.entitySerializer = entitySerializer;
        this.eventSerializer = eventSerializer;
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

    private static class TimedRunnable {
        final Runnable runnable;
        final long time;

        TimedRunnable(Runnable runnable, long time) {
            super();
            this.runnable = runnable;
            this.time = time;
        }
    }

    public void initialize() {
        List<TimedRunnable> list = new ArrayList<TimedRunnable>();
        try (Connection con = createConnection()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "select cls, id, event_bytes, time from delayed_signal_queue order by seq_num")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String className = rs.getString("cls");
                        String id = rs.getString("id");
                        byte[] eventBytes = readAll(rs.getBlob("event_bytes").getBinaryStream());
                        Object event = eventSerializer.deserialize(eventBytes);
                        Class<?> cls = Class.forName(className);
                        long time = rs.getTimestamp("times").getTime();
                        @SuppressWarnings("unchecked")
                        Signal<?, String> signal = Signal.create((Class<Object>) cls, id,
                                (Event<Object>) event);
                        list.add(new TimedRunnable(() -> {
                            offer(signal);
                        } , time));
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
        for (TimedRunnable r : list) {
            long now = clock.now();
            long delayMs = Math.max(0, r.time - now);
            executor.schedule(r.runnable, delayMs, TimeUnit.MILLISECONDS);
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
    public void process(Signal<?, String> signal) {
        try (Connection con = createConnection()) {
            con.setAutoCommit(false);
            EntityStateMachine<?, String> esm = null;
            insertIntoSignalStore(esm, signal.event(), eventSerializer(), con);
            @SuppressWarnings("unchecked")
            EntityStateMachine<?, String> esm2 = esm.signal((Event<Object>) signal.event());
            List<Signal<?, ?>> signalsToOther = esm2.signalsToOther();
            insertSignalsToOther(eventSerializer(), con, signalsToOther);
            insertDelayedSignalsToOther(eventSerializer(), con, signalsToOther);
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

    private void insertIntoSignalStore(EntityStateMachine<?, String> esm, Event<?> event,
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
    public void replay(Class<?> cls, String id) {
        try (Connection con = createConnection()) {
            return;
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
    public <T> Optional<T> get(Class<T> cls, String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Serializer entitySerializer() {
        return eventSerializer;
    }

    @Override
    public Serializer eventSerializer() {
        return entitySerializer;
    }

    @Override
    public void offer(Signal<?, String> signal) {
        queue.offer(signal);
        drain();
    }

    private void drain() {
        if (wip.getAndIncrement() == 0) {
            int missed = 1;
            while (true) {
                while (true) {
                    Signal<?, ?> signal = queue.poll();
                    if (signal == null) {
                        break;
                    } else {

                    }
                }
                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    return;
                }
            }
        }
    }

}
