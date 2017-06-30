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
import com.github.davidmoten.guavamini.Preconditions;

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

    @SuppressWarnings("unchecked")
    public void initialize() {
        List<NumberedSignal<?, ?>> list = new ArrayList<NumberedSignal<?, ?>>();
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
                                (Event<Object>) event, Optional.of(time));
                        list.add(new NumberedSignal<Object, String>(signal, number));
                    }
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
        for (NumberedSignal<?, ?> sig : list) {
            schedule(sig);
        }
    }

    @SuppressWarnings("unchecked")
    private void schedule(NumberedSignal<?, ?> sig) {
        Preconditions.checkArgument(sig.signal.time().isPresent());
        long now = clock.now();
        long delayMs = Math.max(0, sig.signal.time().get() - now);
        executor.schedule(() -> offer((NumberedSignal<?, String>) sig), delayMs,
                TimeUnit.MILLISECONDS);
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
        List<NumberedSignal<?, ?>> delayedNumberedSignalsToOther;
        try (Connection con = createConnection()) {
            // start a transaction
            con.setAutoCommit(false);

            // if signal does not exist in queue anymore then ignore
            if (!signalExists(con, signal)) {
                return;
            }

            // get behaviour
            EntityBehaviour<Object, String> behaviour = (EntityBehaviour<Object, String>) behaviourFactory
                    .apply(signal.signal.cls());

            // read entity
            Optional<EntityAndState<Object>> entity = readEntity(con,
                    (Class<Object>) signal.signal.cls(), signal.signal.id(),
                    (EntityBehaviour<Object, String>) behaviour);

            // initialize state machine
            final EntityStateMachine<?, String> esm = getStateMachine(signal, behaviour, entity);

            // apend signal to signal_store
            insertIntoSignalStore(con, esm, signal.signal.event(), eventSerializer());

            // push signal through state machine
            EntityStateMachine<?, String> esm2 = esm.signal((Event<Object>) signal.signal.event());

            List<Signal<?, ?>> signalsToOther = esm2.signalsToOther();

            // add signals to others to signal_queue
            numberedSignalsToOther = insertSignalsToOther(con, eventSerializer(), signalsToOther);

            // add delayed signals to other to delayed_signal_queue
            delayedNumberedSignalsToOther = insertDelayedSignalsToOther(con, esm2.cls(), esm.id(),
                    eventSerializer(), signalsToOther);

            // remove signal from signal_queue
            removeSignal(con, signal);

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
        for (NumberedSignal<?, ?> signalToOther : delayedNumberedSignalsToOther) {
            schedule(signalToOther);
        }
    }

    private boolean signalExists(Connection con, NumberedSignal<?, String> signal)
            throws SQLException {
        if (signal.signal.time().isPresent()) {
            try (PreparedStatement ps = con
                    .prepareStatement("select 0 from delayed_signal_queue where seq_num=?")) {
                ps.setLong(1, signal.number);
                ResultSet rs = ps.executeQuery();
                return rs.next();
            }
        } else {
            try (PreparedStatement ps = con
                    .prepareStatement("select 0 from signal_queue where seq_num=?")) {
                ps.setLong(1, signal.number);
                ResultSet rs = ps.executeQuery();
                return rs.next();
            }
        }
    }

    private EntityStateMachine<?, String> getStateMachine(NumberedSignal<?, String> signal,
            EntityBehaviour<Object, String> behaviour, Optional<EntityAndState<Object>> entity) {
        final EntityStateMachine<?, String> esm;
        if (entity.isPresent()) {
            esm = behaviour.create(signal.signal.id());
        } else {
            esm = behaviour.create(signal.signal.id(), entity.get().entity, entity.get().state);
        }
        return esm;
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

    private void insertIntoSignalStore(Connection con, EntityStateMachine<?, String> esm,
            Event<?> event, Serializer eventSerializer) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(
                "insert into signal_store(cls, id, event_cls, event_bytes) values(?,?,?,?)")) {
            ps.setString(1, esm.cls().getName());
            ps.setString(2, esm.id());
            ps.setString(3, event.getClass().getName());
            ps.setBlob(4, new ByteArrayInputStream(eventSerializer.serialize(event)));
            ps.executeUpdate();
        }
    }

    @SuppressWarnings("unchecked")
    private List<NumberedSignal<?, ?>> insertSignalsToOther(Connection con,
            Serializer eventSerializer, List<Signal<?, ?>> signalsToOther) throws SQLException {
        List<NumberedSignal<?, ?>> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "insert into signal_queue(cls, id, event_cls, event_bytes) values(?,?,?,?)")) {
            for (Signal<?, ?> signal : signalsToOther) {
                if (!signal.time().isPresent()) {
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

    @SuppressWarnings("unchecked")
    private List<NumberedSignal<?, ?>> insertDelayedSignalsToOther(Connection con, Class<?> fromCls,
            String fromId, Serializer eventSerializer, List<Signal<?, ?>> signalsToOther)
                    throws SQLException {
        List<NumberedSignal<?, ?>> list = new ArrayList<NumberedSignal<?, ?>>();
        try ( //
                PreparedStatement ps = con.prepareStatement(
                        "insert into delayed_signal_queue(from_cls, from_id, ls, id, event_cls, event_bytes, time) values(?,?,?,?,?,?,?)");
                PreparedStatement del = con.prepareStatement(
                        "delete from delayed_signal_queue where from_cls=? and from_id=? and cls=? and id=?")) {

            for (Signal<?, ?> signal : signalsToOther) {
                if (signal.time().isPresent()) {
                    Signal<?, String> sig = (Signal<?, String>) signal;
                    del.setString(1, fromCls.getName());
                    del.setString(2, fromId);
                    del.setString(3, sig.cls().getName());
                    del.setString(4, sig.id());
                    del.executeUpdate();
                    ps.setString(1, fromCls.getName());
                    ps.setString(2, fromId);
                    ps.setString(3, sig.cls().getName());
                    ps.setString(4, sig.id());
                    ps.setString(5, sig.event().getClass().getName());
                    ps.setBlob(6, new ByteArrayInputStream(eventSerializer.serialize(sig.event())));
                    ps.setTimestamp(7, new Timestamp(sig.time().get()));
                    ps.executeUpdate();
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

    private void removeSignal(Connection con, NumberedSignal<?, String> signal)
            throws SQLException {
        if (signal.signal.time().isPresent()) {
            try (PreparedStatement ps = con
                    .prepareStatement("delete from delayed_signal_queue where seq_num=?")) {
                ps.setLong(1, signal.number);
                ps.executeUpdate();
            }
        } else {
            try (PreparedStatement ps = con
                    .prepareStatement("delete from ssignal_queue where seq_num=?")) {
                ps.setLong(1, signal.number);
                ps.executeUpdate();
            }
        }
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
