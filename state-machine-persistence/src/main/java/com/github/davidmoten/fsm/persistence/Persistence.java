package com.github.davidmoten.fsm.persistence;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.github.davidmoten.fsm.runtime.CancelTimedSignal;
import com.github.davidmoten.fsm.runtime.Clock;
import com.github.davidmoten.fsm.runtime.ClockDefault;
import com.github.davidmoten.fsm.runtime.Create;
import com.github.davidmoten.fsm.runtime.EntityBehaviour;
import com.github.davidmoten.fsm.runtime.EntityState;
import com.github.davidmoten.fsm.runtime.EntityStateMachine;
import com.github.davidmoten.fsm.runtime.Event;
import com.github.davidmoten.fsm.runtime.Signal;
import com.github.davidmoten.guavamini.Preconditions;

public final class Persistence {

    private final ScheduledExecutorService executor;
    private final Clock clock;
    private final Serializer entitySerializer;
    private final Serializer eventSerializer;
    private final Sql sql;
    private final Callable<Connection> connectionFactory;
    private final Function<Class<?>, EntityBehaviour<?, String>> behaviourFactory;
    private final Queue<NumberedSignal<?, ?>> queue = new LinkedList<>();
    private final AtomicInteger wip = new AtomicInteger();
    private final boolean storeSignals;

    private Persistence(ScheduledExecutorService executor, Clock clock, Serializer entitySerializer,
            Serializer eventSerializer, Function<Class<?>, EntityBehaviour<?, String>> behaviourFactory, Sql sql,
            Callable<Connection> connectionFactory, boolean storeSignals) {
        this.executor = executor;
        this.clock = clock;
        this.entitySerializer = entitySerializer;
        this.eventSerializer = eventSerializer;
        this.behaviourFactory = behaviourFactory;
        this.sql = sql;
        this.connectionFactory = connectionFactory;
        this.storeSignals = storeSignals;
    }

    public static Builder connectionFactory(Callable<Connection> connectionFactory) {
        return new Builder().connectionFactory(connectionFactory);
    }

    public static final class Builder {
        private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        private Clock clock = ClockDefault.instance();
        private Serializer entitySerializer = Serializer.JSON;
        private Serializer eventSerializer = Serializer.JSON;
        private Function<Class<?>, EntityBehaviour<?, String>> behaviourFactory;
        private Sql sql = Sql.DEFAULT;
        private Callable<Connection> connectionFactory;
        private boolean storeSignals = true;

        private Builder() {
            // do nothing
        }

        public Builder executor(ScheduledExecutorService executor) {
            this.executor = executor;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder entitySerializer(Serializer serializer) {
            this.entitySerializer = serializer;
            return this;
        }

        public Builder eventSerializer(Serializer serializer) {
            this.eventSerializer = serializer;
            return this;
        }

        public Builder behaviourFactory(Function<Class<?>, EntityBehaviour<?, String>> behaviourFactory) {
            this.behaviourFactory = behaviourFactory;
            return this;
        }

        public Builder sql(Sql sql) {
            this.sql = sql;
            return this;
        }

        public Builder connectionFactory(Callable<Connection> connectionFactory) {
            this.connectionFactory = connectionFactory;
            return this;
        }

        public Builder storeSignals(boolean storeSignals) {
            this.storeSignals = storeSignals;
            return this;
        }

        public Persistence build() {
            return new Persistence(executor, clock, entitySerializer, eventSerializer, behaviourFactory, sql,
                    connectionFactory, storeSignals);
        }
    }

    public void create() {
        String sql = new String(readAll(Persistence.class.getResourceAsStream("/create-h2.sql")),
                StandardCharsets.UTF_8);
        create(sql);
    }

    public void create(String sql) {
        try (Connection con = createConnection()) {
            con.setAutoCommit(true);
            String[] commands = sql.split(";");
            for (String command : commands) {
                con.prepareStatement(command).execute();
            }
            con.commit();
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    public <T> void signal(Class<T> cls, String id, Event<? super T> event) {
        signal(Signal.create(cls, id, event));
    }

    public void signal(Signal<?, String> signal) {

        if (!signal.time().isPresent()) {
            try ( //
                    Connection con = createConnection();
                    PreparedStatement ps = con.prepareStatement(sql.addToSignalQueue())) {
                ps.setString(1, signal.cls().getName());
                ps.setString(2, signal.id());
                ps.setString(3, signal.event().getClass().getName());
                ps.setBlob(4, new ByteArrayInputStream(eventSerializer.serialize(signal.event())));
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    long number = rs.getLong(1);
                    offer(new NumberedSignal<>(signal, number));
                }
            } catch (SQLException e) {
                throw new SQLRuntimeException(e);
            }
        } else {

        }
    }

    @SuppressWarnings("unchecked")
    public void initialize() {
        List<NumberedSignal<?, ?>> list = new ArrayList<NumberedSignal<?, ?>>();
        try ( //
                Connection con = createConnection();
                PreparedStatement ps = con.prepareStatement(sql.selectDelayedSignals())) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long number = rs.getLong("seq_num");
                    String className = rs.getString("cls");
                    String id = rs.getString("id");
                    byte[] eventBytes = readAll(rs.getBlob("event_bytes").getBinaryStream());
                    String eventClsName = rs.getString("event_cls");
                    Class<?> eventClass = Class.forName(eventClsName);
                    Object event = eventSerializer.deserialize(eventClass, eventBytes);
                    Class<?> cls = Class.forName(className);
                    long time = rs.getTimestamp("times").getTime();
                    Signal<Object, String> signal = Signal.create((Class<Object>) cls, id, (Event<Object>) event,
                            Optional.of(time));
                    list.add(new NumberedSignal<Object, String>(signal, number));
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
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
        executor.schedule(() -> offer((NumberedSignal<?, String>) sig), delayMs, TimeUnit.MILLISECONDS);
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

    public static final class EntityAndState<T> {
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

            // if is cancellation then remove signal from delayed queue and
            // return
            if (handleCancellationSignal(con, signal)) {
                return;
            }

            // get behaviour
            EntityBehaviour<Object, String> behaviour = (EntityBehaviour<Object, String>) behaviourFactory
                    .apply(signal.signal.cls());

            // read entity
            Optional<EntityAndState<Object>> entity = readEntity(con, (Class<Object>) signal.signal.cls(),
                    signal.signal.id(), (EntityBehaviour<Object, String>) behaviour);

            // initialize state machine
            final EntityStateMachine<?, String> esm = getStateMachine(signal, behaviour, entity);

            // apend signal to signal_store (optional)
            if (storeSignals) {
                insertIntoSignalStore(con, esm, signal.signal.event(), eventSerializer);
            }

            Signals<String> signals = new Signals<String>();
            signals.signalsToSelf.offerFirst(signal.signal.event());

            // push signal through state machine which will immediately process
            // non-delayed signals to self and accumulate signals to others
            EntityStateMachine<?, String> esm2 = esm;
            Event<?> event;
            while ((event = signals.signalsToSelf.poll()) != null) {
                esm2 = esm2.signal((Event<Object>) event);
                List<Event<Object>> list = (List<Event<Object>>) (List<?>) esm2.signalsToSelf();
                for (int i = list.size() - 1; i >= 0; i--) {
                    signals.signalsToSelf.offerLast(list.get(i));
                }
                for (Signal<?, ?> s : esm2.signalsToOther()) {
                    signals.signalsToOther.offerFirst((Signal<?, String>) s);
                }
            }

            Deque<Signal<?, String>> signalsToOther = signals.signalsToOther;

            // add signals to others to signal_queue
            numberedSignalsToOther = insertSignalsToOther(con, eventSerializer, signalsToOther);

            // add delayed signals to other to delayed_signal_queue
            delayedNumberedSignalsToOther = insertDelayedSignalsToOther(con, esm2.cls(), esm.id(), eventSerializer,
                    signalsToOther);

            // remove signal from signal_queue
            removeSignal(con, signal);

            // update/create the entity bytes and state to entity table
            saveEntity(con, esm2);

            // commit the transaction
            con.commit();
            System.out.println(signal.signal.cls().getSimpleName() + "[" + signal.signal.id() + "] - " + esm2.state());
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

    private static final class Signals<Id> {
        final Deque<Event<?>> signalsToSelf = new ArrayDeque<>();
        final Deque<Signal<?, Id>> signalsToOther = new ArrayDeque<>();
    }

    private boolean handleCancellationSignal(Connection con, NumberedSignal<?, String> signal) throws SQLException {
        if (signal.signal.event() instanceof CancelTimedSignal) {
            @SuppressWarnings("unchecked")
            CancelTimedSignal<String> c = (CancelTimedSignal<String>) signal.signal.event();
            removeDelayedSignal(con, c.fromClass(), c.fromId(), signal.signal.cls(), signal.signal.id());
            return true;
        } else {
            return false;
        }
    }

    private boolean signalExists(Connection con, NumberedSignal<?, String> signal) throws SQLException {
        if (signal.signal.time().isPresent()) {
            try (PreparedStatement ps = con.prepareStatement(sql.delayedSignalExists())) {
                ps.setLong(1, signal.number);
                ResultSet rs = ps.executeQuery();
                return rs.next();
            }
        } else {
            try (PreparedStatement ps = con.prepareStatement(sql.signalExists())) {
                ps.setLong(1, signal.number);
                ResultSet rs = ps.executeQuery();
                return rs.next();
            }
        }
    }

    private EntityStateMachine<?, String> getStateMachine(NumberedSignal<?, String> signal,
            EntityBehaviour<Object, String> behaviour, Optional<EntityAndState<Object>> entity) {
        if (!entity.isPresent()) {
            return behaviour.create(signal.signal.id()).signal(new Create());
        } else {
            return behaviour.create(signal.signal.id(), entity.get().entity, entity.get().state);
        }
    }

    private <T> Optional<EntityAndState<T>> readEntity(Connection con, Class<T> cls, String id,
            EntityBehaviour<T, String> behaviour) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql.readEntityAndState())) {
            ps.setString(1, cls.getName());
            ps.setString(2, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                return Optional.empty();
            } else {
                byte[] bytes = readAll(rs.getBlob("bytes").getBinaryStream());
                T entity = (T) entitySerializer.deserialize(cls, bytes);
                EntityState<T> state = behaviour.from(rs.getString("state"));
                return Optional.of(EntityAndState.create(entity, state));
            }
        }
    }

    private void insertIntoSignalStore(Connection con, EntityStateMachine<?, String> esm, Event<?> event,
            Serializer eventSerializer) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql.addToSignalStore())) {
            ps.setString(1, esm.cls().getName());
            ps.setString(2, esm.id());
            ps.setString(3, event.getClass().getName());
            ps.setBlob(4, new ByteArrayInputStream(eventSerializer.serialize(event)));
            ps.executeUpdate();
        }
    }

    @SuppressWarnings("unchecked")
    private List<NumberedSignal<?, ?>> insertSignalsToOther(Connection con, Serializer eventSerializer,
            Collection<Signal<?, String>> signalsToOther) throws SQLException {
        List<NumberedSignal<?, ?>> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(sql.addToSignalQueue())) {
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
                        list.add(new NumberedSignal<Object, String>((Signal<Object, String>) signal, rs.getLong(1)));
                    }
                }
            }
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private List<NumberedSignal<?, ?>> insertDelayedSignalsToOther(Connection con, Class<?> fromCls, String fromId,
            Serializer eventSerializer, Collection<Signal<?, String>> signalsToOther) throws SQLException {
        List<NumberedSignal<?, ?>> list = new ArrayList<NumberedSignal<?, ?>>();
        try ( //
                PreparedStatement del = con.prepareStatement(sql.deleteDelayedSignal());
                PreparedStatement ps = con.prepareStatement(sql.addDelayedSignal())) {

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
                        list.add(new NumberedSignal<Object, String>((Signal<Object, String>) signal, rs.getLong(1)));
                    }
                }
            }
        }
        return list;
    }

    private void removeSignal(Connection con, NumberedSignal<?, String> signal) throws SQLException {
        if (signal.signal.time().isPresent()) {
            removeNonDelayedSignal(con, signal.number);
        } else {
            removeDelayedSignal(con, signal.number);
        }
    }

    private void removeNonDelayedSignal(Connection con, long number) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql.deleteNumberedSignal())) {
            ps.setLong(1, number);
            ps.executeUpdate();
        }
    }

    private void removeDelayedSignal(Connection con, long number) throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql.deleteNumberedDelayedSignal())) {
            ps.setLong(1, number);
            ps.executeUpdate();
        }
    }

    private void removeDelayedSignal(Connection con, Class<?> fromClass, String fromId, Class<?> cls, String id)
            throws SQLException {
        try (PreparedStatement ps = con.prepareStatement(sql.deleteDelayedSignal())) {
            ps.setString(1, fromClass.getName());
            ps.setString(2, fromId);
            ps.setString(3, cls.getName());
            ps.setString(4, id);
            ps.executeUpdate();
        }
    }

    private void saveEntity(Connection con, EntityStateMachine<?, String> esm) throws SQLException {
        final boolean updated;
        try (PreparedStatement ps = con.prepareStatement(sql.updateEntity())) {
            byte[] bytes = entitySerializer.serialize(esm.get().get());
            ps.setBlob(1, new ByteArrayInputStream(bytes));
            ps.setString(2, esm.state().toString());
            ps.setString(3, esm.cls().getName());
            ps.setString(4, esm.id());
            updated = ps.executeUpdate() > 0;
        }
        if (!updated) {
            try (PreparedStatement ps = con.prepareStatement(sql.insertEntity())) {
                byte[] bytes = entitySerializer.serialize(esm.get().get());
                ps.setString(1, esm.cls().getName());
                ps.setString(2, esm.id());
                ps.setBlob(3, new ByteArrayInputStream(bytes));
                ps.setString(4, esm.state().toString());
                ps.executeUpdate();
            }
        }
    }

    public void replay(Class<?> cls, String id) {
        try (Connection con = createConnection()) {
            return;
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private Connection createConnection() {
        try {
            return connectionFactory.call();
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<EntityAndState<T>> getWithState(Class<T> cls, String id) {
        try ( //
                Connection con = createConnection();
                PreparedStatement ps = con.prepareStatement(sql.readEntityAndState())) {
            ps.setString(1, cls.getName());
            ps.setString(2, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String stateName = rs.getString(1);
                T t = (T) entitySerializer.deserialize(cls, readAll(rs.getBlob(2).getBinaryStream()));
                EntityBehaviour<?, String> behaviour = behaviourFactory.apply(cls);
                EntityState<?> state = behaviour.from(stateName);
                return Optional.of(EntityAndState.create(t, (EntityState<T>) state));
            } else {
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    public <T> Optional<T> get(Class<T> cls, String id) {
        try (Connection con = createConnection(); PreparedStatement ps = con.prepareStatement(sql.readEntity())) {
            ps.setString(1, cls.getName());
            ps.setString(2, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                T t = (T) entitySerializer.deserialize(cls, readAll(rs.getBlob(1).getBinaryStream()));
                return Optional.of(t);
            } else {
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new SQLRuntimeException(e);
        }
    }

    private void offer(NumberedSignal<?, String> signal) {
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
