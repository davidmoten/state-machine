package com.github.davidmoten.fsm.persistence;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.Test;

import com.github.davidmoten.fsm.example.generated.MicrowaveBehaviour;
import com.github.davidmoten.fsm.example.generated.MicrowaveBehaviourBase;
import com.github.davidmoten.fsm.example.generated.MicrowaveStateMachine;
import com.github.davidmoten.fsm.example.microwave.Microwave;
import com.github.davidmoten.fsm.example.microwave.event.ButtonPressed;
import com.github.davidmoten.fsm.example.microwave.event.DoorClosed;
import com.github.davidmoten.fsm.example.microwave.event.DoorOpened;
import com.github.davidmoten.fsm.example.microwave.event.TimerTimesOut;
import com.github.davidmoten.fsm.runtime.ClockDefault;
import com.github.davidmoten.fsm.runtime.Create;
import com.github.davidmoten.fsm.runtime.EntityBehaviour;
import com.github.davidmoten.fsm.runtime.Event;
import com.github.davidmoten.fsm.runtime.Signal;
import com.github.davidmoten.fsm.runtime.Signaller;

public class PersistenceMicrowaveTest {

    @Test
    public void testEventSerializerRoundTrip() {
        Serializer s = createMicrowaveEventSerializer();
        assertTrue(s.deserialize(DoorOpened.class, s.serialize(new DoorOpened())) instanceof DoorOpened);
    }

    @Test
    public void testEventSerializerRoundTripCreate() {
        Serializer s = createMicrowaveEventSerializer();
        assertTrue(s.deserialize(Create.class, s.serialize(new Create())) instanceof Create);
    }

    @Test
    public void testMicrowaveSerializerRoundTrip() {
        Serializer s = createMicrowaveSerializer();
        assertTrue(s.deserialize(Microwave.class, s.serialize(new Microwave(1))) instanceof Microwave);
    }

    @Test
    public void test() throws IOException {
        File directory = File.createTempFile("db-", "", new File("target"));
        directory.mkdir();
        Serializer entitySerializer = createMicrowaveSerializer();
        Serializer eventSerializer = createMicrowaveEventSerializer();
        MicrowaveBehaviour<String> behaviour = createMicrowaveBehaviour();
        Function<Class<?>, EntityBehaviour<?, String>> behaviourFactory = cls -> behaviour;
        TestExecutor executor = new TestExecutor();
        PersistenceH2 p = new PersistenceH2(directory, executor, ClockDefault.instance(), entitySerializer,
                eventSerializer, behaviourFactory);
        p.create();
        p.initialize();
        assertFalse(p.get(Microwave.class, "1").isPresent());
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
    }

    private static void signal(PersistenceH2 p, Event<Microwave> event) {
        p.signal(Signal.create(Microwave.class, "1", event));
    }

    private static void check(PersistenceH2 p, MicrowaveStateMachine.State state) {
        assertEquals(state, p.get(Microwave.class, "1").get().state);
    }

    private static MicrowaveBehaviour<String> createMicrowaveBehaviour() {
        MicrowaveBehaviour<String> behaviour = new MicrowaveBehaviourBase<String>() {

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
        return behaviour;
    }

    private static Serializer createMicrowaveEventSerializer() {
        Serializer eventSerializer = new Serializer() {

            @Override
            public byte[] serialize(Object t) {
                // Event<Microwave> event = (Event<Microwave>) t;
                String className = t.getClass().getName();
                return className.getBytes(UTF_8);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T deserialize(Class<T> cls, byte[] bytes) {
                String className = new String(bytes, UTF_8);
                try {
                    return (T) Class.forName(className).newInstance();
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        return eventSerializer;
    }

    private static Serializer createMicrowaveSerializer() {
        Serializer entitySerializer = new Serializer() {

            @Override
            public byte[] serialize(Object t) {
                Microwave m = (Microwave) t;
                return String.valueOf(m.serialNumber()).getBytes(UTF_8);
            }

            @SuppressWarnings("unchecked")
            @Override
            public <T> T deserialize(Class<T> cls, byte[] bytes) {
                int serialNumber = Integer.parseInt(new String(bytes, UTF_8));
                return (T) new Microwave(serialNumber);
            }
        };
        return entitySerializer;
    }

}
