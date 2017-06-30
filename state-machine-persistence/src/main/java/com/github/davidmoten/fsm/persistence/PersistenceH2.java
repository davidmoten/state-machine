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
import java.util.function.Function;

import com.github.davidmoten.fsm.runtime.Clock;
import com.github.davidmoten.fsm.runtime.EntityBehaviour;
import com.github.davidmoten.fsm.runtime.EntityState;
import com.github.davidmoten.fsm.runtime.EntityStateMachine;
import com.github.davidmoten.fsm.runtime.Event;
import com.github.davidmoten.fsm.runtime.Signal;

public final class PersistenceH2 implements Persistence {

    private final File directory;
    private final ScheduledExecutorService executor;
    private final Clock clock;
    private final Serializer entitySerializer;
    private final Serializer eventSerializer;
    private final Queue<NumberedSignal<?, ?>> queue = new LinkedList<>();
    private final AtomicInteger wip = new AtomicInteger();
    private final Function<Class<?>, EntityBehaviour<?, String>> behaviourFactory;

    public PersistenceH2(File directory, ScheduledExecutorService executor, Clock clock,
            Serializer entitySerializer, Serializer eventSerializer,
            Function<Class<?>, EntityBehaviour<?, String>> behaviourFactory) {
        this.directory = directory;
        this.executor = executor;
        this.clock = clock;
        this.entitySerializer = entitySerializer;
        this.eventSerializer = eventSerializer;
        this.behaviourFactory = behaviourFactory;
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
            con.commit();
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private static final class NumberedDelayedSignal<T, Id> {
        final NumberedSignal<T, Id> signal;
        final long time;

        NumberedDelayedSignal(NumberedSignal<T, Id> signal, long time) {
            this.signal = signal;
            this.time = time;
        }
    }

    @SuppressWarnings("unchecked")
    public void initialize() {
        List<NumberedDelayedSignal<?, ?>> list = new ArrayList<NumberedDelayedSignal<?, ?>>();
        try (Connection con = createConnection()) {
            try (PreparedStatement ps = con.prepareStatement(
                    "select seq_num, cls, id, event_bytes, time from delayed_signal_queue order by seq_num")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long number = rs.getLong("seq_num");
                        String className = rs.getString("cls");
                        String id = rs.getString("id");
                        byte[] eventBytes = readAll(rs.getBlob("event_bytes").getBinaryStream());
                        Object event = eventSerializer.deserialize(eventBytes);
                        Class<?> cls = Class.forName(className);
                        long time = rs.getTimestamp("times").getTime();
                        Signal<Object, String> signal = Signal.create((Class<Object>) cls, id,
                                (Event<Object>) event);
                        list.add(new NumberedDelayedSignal<Object, String>(
                                new NumberedSignal<Object, String>(signal, number), time));
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
        for (NumberedDelayedSignal<?, ?> sig : list) {
            long now = clock.now();
            long delayMs = Math.max(0, sig.time - now);
            executor.schedule(() -> offer((NumberedSignal<?, String>) sig.signal), delayMs,
                    TimeUnit.MILLISECONDS);
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

    private static final class EntityAndState<T> {
        final T entity;
        final EntityState<T> state;

        EntityAndState(T entity, EntityState<T> state) {
            this.entity = entity;
            this.state = state;
        }

        static <T> EntityAndState<T> create(T entity, EntityState<T> state) {
            return new EntityAndState<T>(entity, state);
        }
    }

    @SuppressWarnings("unchecked")
    private void process(NumberedSignal<?, String> signal) {
        List<NumberedSignal<?, ?>> numberedSignalsToOther;
        try (Connection con = createConnection()) {
            con.setAutoCommit(false);

            // get behaviour
            EntityBehaviour<Object, String> behaviour = (EntityBehaviour<Object, String>) behaviourFactory
                    .apply(signal.signal.cls());

            // read entity
            Optional<EntityAndState<Object>> entity = readEntity(con,
                    (Class<Object>) signal.signal.cls(), signal.signal.id(),
                    (EntityBehaviour<Object, String>) behaviour);

            // initialize state machine
            final EntityStateMachine<?, String> esm;
            if (entity.isPresent()) {
                esm = behaviour.create(signal.signal.id());
            } else {
                esm = behaviour.create(signal.signal.id(), entity.get().entity, entity.get().state);
            }

            // apend signal to signal_store
            insertIntoSignalStore(esm, signal.signal.event(), eventSerializer(), con);

            // push signal through state machine
            EntityStateMachine<?, String> esm2 = esm.signal((Event<Object>) signal.signal.event());

            // add signals to others to signal_queue
            List<Signal<?, ?>> signalsToOther = esm2.signalsToOther();
            numberedSignalsToOther = insertSignalsToOther(eventSerializer(), con, signalsToOther);

            // add delayed signals to other to delayed_signal_queue
            insertDelayedSignalsToOther(eventSerializer(), con, signalsToOther);

            // remove signal from signal_queue
            removeSignal(signal.number, con);

            // update/create the entity bytes and state to entity table
            saveEntity(con, esm2);

            // commit the transaction
            con.commit();
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
        for (NumberedSignal<?, ?> signalToOther : numberedSignalsToOther) {
            offer((NumberedSignal<?, String>) signalToOther);
        }
        // TODO call executor with delayed signals
    }

    private void saveEntity(Connection con, EntityStateMachine<?, String> esm) throws SQLException {
        final boolean updated;
        try (PreparedStatement ps = con
                .prepareStatement("update entity set bytes=?, state=? where cls=? and id=?")) {
            byte[] bytes = entitySerializer.serialize(esm.get().get());
            ps.setBlob(1, new ByteArrayInputStream(bytes));
            ps.setString(2, esm.state().toString());
            ps.setString(3, esm.cls().getName());
            ps.setString(4, esm.id());
            updated = ps.executeUpdate() > 0;
        }
        if (!updated) {
            try (PreparedStatement ps = con.prepareStatement(
                    "insert into entity(cls, id, bytes, state) values(?,?,?,?)")) {
                byte[] bytes = entitySerializer.serialize(esm.get().get());
                ps.setString(1, esm.cls().getName());
                ps.setString(2, esm.id());
                ps.setBlob(3, new ByteArrayInputStream(bytes));
                ps.setString(4, esm.state().toString());
                ps.executeUpdate();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> Optional<EntityAndState<T>> readEntity(Connection con, Class<T> cls, String id,
            EntityBehaviour<T, String> behaviour) throws SQLException {
        try (PreparedStatement ps = con
                .prepareStatement("select state, bytes from entity where cls=? and id=?")) {
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return Optional.empty();
            } else {
                byte[] bytes = readAll(rs.getBlob("bytes").getBinaryStream());
                T entity = (T) entitySerializer.deserialize(bytes);
                EntityState<T> state = behaviour.from(rs.getString("state"));
                return Optional.of(EntityAndState.create(entity, state));
            }
        }
    }

    private void removeSignal(long number, Connection con) throws SQLException {
        try (PreparedStatement ps = con
                .prepareStatement("delete from signal_queue where seq_num=?")) {
            ps.setLong(1, number);
            ps.executeUpdate();
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

    @SuppressWarnings("unchecked")
    private List<NumberedSignal<?, ?>> insertSignalsToOther(Serializer eventSerializer,
            Connection con, List<Signal<?, ?>> signalsToOther) throws SQLException {
        List<NumberedSignal<?, ?>> list = new ArrayList<>();
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
                    // add the generated primary key for the signal to the list
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        list.add(new NumberedSignal<Object, String>((Signal<Object, String>) signal,
                                rs.getLong(0)));
                    }
                }
            }
        }
        return list;
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
    public void offer(NumberedSignal<?, String> signal) {
        queue.offer(signal);
        drain();
    }

    @SuppressWarnings("unchecked")
    private void drain() {
        if (wip.getAndIncrement() == 0) {
            int missed = 1;
            while (true) {
                while (true) {
                    NumberedSignal<?, ?> signal = queue.poll();
                    if (signal == null) {
                        break;
                    } else {
                        process((NumberedSignal<?, String>) signal);
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
