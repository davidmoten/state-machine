package com.github.davidmoten.fsm.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
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
import com.github.davidmoten.fsm.runtime.Create;
import com.github.davidmoten.fsm.runtime.EntityBehaviour;
import com.github.davidmoten.fsm.runtime.Event;
import com.github.davidmoten.fsm.runtime.Signal;
import com.github.davidmoten.fsm.runtime.Signaller;
import com.github.davidmoten.fsm.runtime.TestExecutor;

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
        File directory = File.createTempFile("db-", "", new File("target"));
        directory.mkdir();
        MicrowaveBehaviour<String> behaviour = createMicrowaveBehaviour();
        Function<Class<?>, EntityBehaviour<?, String>> behaviourFactory = cls -> behaviour;
        TestExecutor executor = new TestExecutor();
        Callable<Connection> connectionFactory = () -> DriverManager
                .getConnection("jdbc:h2:" + directory.getAbsolutePath());

        Persistence p = Persistence //
                .connectionFactory(connectionFactory) //
                .executor(executor) //
                .entitySerializer(Serializer.JSON) //
                .eventSerializer(Serializer.JSON) //
                .behaviourFactory(behaviourFactory) //
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

    private static void signal(Persistence p, Event<Microwave> event) {
        p.signal(Signal.create(Microwave.class, "1", event));
    }

    private static void check(Persistence p, MicrowaveStateMachine.State state) {
        assertEquals(state, p.getWithState(Microwave.class, "1").get().state);
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

}
