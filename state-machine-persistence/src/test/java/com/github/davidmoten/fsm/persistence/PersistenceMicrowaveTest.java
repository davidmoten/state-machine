package com.github.davidmoten.fsm.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import com.github.davidmoten.fsm.example.generated.MicrowaveBehaviour;
import com.github.davidmoten.fsm.example.generated.MicrowaveBehaviourBase;
import com.github.davidmoten.fsm.example.generated.MicrowaveStateMachine;
import com.github.davidmoten.fsm.example.microwave.Microwave;
import com.github.davidmoten.fsm.example.microwave.event.ButtonPressed;
import com.github.davidmoten.fsm.example.microwave.event.DoorClosed;
import com.github.davidmoten.fsm.example.microwave.event.DoorOpened;
import com.github.davidmoten.fsm.example.microwave.event.TimerTimesOut;
import com.github.davidmoten.fsm.persistence.Persistence.EntityWithId;
import com.github.davidmoten.fsm.runtime.Create;
import com.github.davidmoten.fsm.runtime.EntityBehaviour;
import com.github.davidmoten.fsm.runtime.Event;
import com.github.davidmoten.fsm.runtime.Signal;
import com.github.davidmoten.fsm.runtime.Signaller;
import com.github.davidmoten.fsm.runtime.TestExecutor;
import com.github.davidmoten.guavamini.Lists;

public class PersistenceMicrowaveTest {

    @Test
    public void testEventSerializerRoundTrip() {
        Serializer s = Serializer.JSON;
        assertTrue(s.deserialize(DoorOpened.class, s.serialize(new DoorOpened())) instanceof DoorOpened);
    }

    @Test
    public void testEventSerializerRoundTripCreate() {
        Serializer s = Serializer.JSON;
        assertTrue(s.deserialize(Create.class, s.serialize(new Create())) instanceof Create);
    }

    @Test
    public void testMicrowaveSerializerRoundTrip() {
        Serializer s = Serializer.JSON;
        assertTrue(s.deserialize(Microwave.class, s.serialize(new Microwave(1))) instanceof Microwave);
    }

    @Test
    public void test() throws IOException {
        MicrowaveBehaviour<String> behaviour = createMicrowaveBehaviour();
        Function<Class<?>, EntityBehaviour<?, String>> behaviourFactory = cls -> behaviour;
        TestExecutor executor = new TestExecutor();
        Persistence p = createPersistence() //
                .behaviourFactory(behaviourFactory) //
                .executor(executor) //
                .build();
        p.create();
        p.initialize();
        assertFalse(p.get(Microwave.class, "1").isPresent());
        assertFalse(p.getWithState(Microwave.class, "1").isPresent());
        signal(p, new DoorOpened());
        check(p, MicrowaveStateMachine.State.DOOR_OPEN);
        signal(p, new DoorClosed());
        check(p, MicrowaveStateMachine.State.READY_TO_COOK);
        signal(p, new ButtonPressed());
        check(p, MicrowaveStateMachine.State.COOKING);
        executor.advance(200, TimeUnit.MILLISECONDS);
        signal(p, new DoorOpened());
        check(p, MicrowaveStateMachine.State.COOKING_INTERRUPTED);
        executor.advance(2, TimeUnit.SECONDS);
        signal(p, new DoorClosed());
        check(p, MicrowaveStateMachine.State.READY_TO_COOK);
        signal(p, new ButtonPressed());
        check(p, MicrowaveStateMachine.State.COOKING);
        executor.advance(1, TimeUnit.SECONDS);
        check(p, MicrowaveStateMachine.State.COOKING_COMPLETE);
        Assert.assertNotNull(p.get(Microwave.class, "1").get());
    }

    @Test
    public void testRetry() throws IOException {
        MicrowaveBehaviour<String> behaviour = createMicrowaveBehaviourThatThrows();
        Function<Class<?>, EntityBehaviour<?, String>> behaviourFactory = cls -> behaviour;
        TestExecutor executor = new TestExecutor();
        List<Throwable> list = new ArrayList<>();
        long retryIntervalMs = 5000;
        Persistence p = createPersistence() //
                .behaviourFactory(behaviourFactory) //
                .executor(executor) //
                .errorHandler(t -> list.add(t)) //
                .retryInterval(retryIntervalMs, TimeUnit.MILLISECONDS) //
                .build();
        p.create();
        p.initialize();
        signal(p, new ButtonPressed());
        assertFalse(p.get(Microwave.class, "1").isPresent());
        assertEquals(1, list.size());
        assertEquals("boo", list.get(0).getMessage());
        executor.advance(retryIntervalMs, TimeUnit.MILLISECONDS);
        check(p, MicrowaveStateMachine.State.COOKING);
    }

    @Test
    public void testGetByProperties() throws IOException {
        MicrowaveBehaviour<String> behaviour = createMicrowaveBehaviour();
        Function<Class<?>, EntityBehaviour<?, String>> behaviourFactory = cls -> behaviour;
        TestExecutor executor = new TestExecutor();

        Persistence p = createPersistence() //
                .behaviourFactory(behaviourFactory) //
                .propertiesFactory(Microwave.class, //
                        m -> Lists.newArrayList(Property.create("colour", "white"))) //
                .executor(executor) //
                .build();
        p.create();
        p.initialize();
        signal(p, new DoorOpened());
        Set<EntityWithId<Microwave>> set = p.get(Microwave.class, "colour", "white");
        assertEquals(1, set.size());
    }

    @Test
    public void testRangeQuery() throws IOException {
        MicrowaveBehaviour<String> behaviour = createMicrowaveBehaviour();
        Function<Class<?>, EntityBehaviour<?, String>> behaviourFactory = cls -> behaviour;
        TestExecutor executor = new TestExecutor();

        Persistence p = createPersistence() //
                .behaviourFactory(behaviourFactory) //
                .propertiesFactory(Microwave.class, //
                        m -> Lists.newArrayList(Property.create("colour", "white"))) //
                .rangeMetricFactory(Microwave.class, m -> Optional.of(IntProperty.create("range", 123))) //
                .executor(executor) //
                .build();
        p.create();
        p.initialize();
        signal(p, new DoorOpened());
        Set<EntityWithId<Microwave>> set = p.get(Microwave.class, "colour", "white");
        assertEquals(1, set.size());
        {
            List<EntityWithId<Microwave>> list = p.get(Microwave.class, "colour", "white", "range", 80, true, 140,
                    false, 100, Optional.empty());
            assertEquals(1, list.size());
        }
        {
            List<EntityWithId<Microwave>> list = p.get(Microwave.class, "colour", "white", "range", 150, true, 160,
                    false, 100, Optional.empty());
            assertEquals(0, list.size());
        }
        {
            List<EntityWithId<Microwave>> list = p.get(Microwave.class, "colour", "white", "range", 70, true, 160,
                    false, 100, Optional.of("1"));
            assertEquals(0, list.size());
        }
    }

    private MicrowaveBehaviour<String> createMicrowaveBehaviourThatThrows() {
        return new MicrowaveBehaviourBase<String>() {

            boolean firstTime = true;

            @Override
            public MicrowaveStateMachine<String> create(String id) {
                return MicrowaveStateMachine.create(Microwave.fromId(id), id, this,
                        MicrowaveStateMachine.State.READY_TO_COOK);
            }

            @Override
            public Microwave onEntry_Cooking(Signaller<Microwave, String> signaller, Microwave microwave, String id,
                    ButtonPressed event, boolean replaying) {
                System.out.println(Entities.get().get(Microwave.class).size() + " entities found");
                if (firstTime) {
                    firstTime = false;
                    throw new RuntimeException("boo");
                }
                return microwave;
            }

        };
    }

    private static Persistence.Builder createPersistence() throws IOException {
        File directory = File.createTempFile("db-", "", new File("target"));
        directory.mkdir();
        Callable<Connection> connectionFactory = () -> DriverManager
                .getConnection("jdbc:h2:" + directory.getAbsolutePath());

        return Persistence //
                .connectionFactory(connectionFactory) //
                .errorHandlerPrintStackTraceAndThrow() //
                .entitySerializer(Serializer.JSON) //
                .eventSerializer(Serializer.JSON);

    }

    private static void signal(Persistence p, Event<Microwave> event) {
        p.signal(Signal.create(Microwave.class, "1", event));
    }

    private static void check(Persistence p, MicrowaveStateMachine.State state) {
        assertEquals(state, p.getWithState(Microwave.class, "1").get().state);
    }

    private static MicrowaveBehaviour<String> createMicrowaveBehaviour() {
        return new MicrowaveBehaviourBase<String>() {

            @Override
            public MicrowaveStateMachine<String> create(String id) {
                return MicrowaveStateMachine.create(Microwave.fromId(id), id, this,
                        MicrowaveStateMachine.State.READY_TO_COOK);
            }

            @Override
            public Microwave onEntry_Cooking(Signaller<Microwave, String> signaller, Microwave microwave, String id,
                    ButtonPressed event, boolean replaying) {
                if (!replaying) {
                    signaller.signalToSelf(new TimerTimesOut(), 1, TimeUnit.SECONDS);
                }
                return microwave;
            }

        };
    }

}
