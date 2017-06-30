package com.github.davidmoten.fsm.persistence;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import com.github.davidmoten.fsm.example.generated.MicrowaveBehaviour;
import com.github.davidmoten.fsm.example.generated.MicrowaveBehaviourBase;
import com.github.davidmoten.fsm.example.generated.MicrowaveStateMachine;
import com.github.davidmoten.fsm.example.microwave.Microwave;
import com.github.davidmoten.fsm.example.microwave.event.DoorOpened;
import com.github.davidmoten.fsm.runtime.ClockDefault;
import com.github.davidmoten.fsm.runtime.Create;
import com.github.davidmoten.fsm.runtime.EntityBehaviour;

public class PersistenceH2Test {
    
    @Test
    public void testEventSerializerRoundTrip() {
        Serializer s = createMicrowaveEventSerializer();
        assertTrue(s.deserialize(s.serialize(new DoorOpened())) instanceof DoorOpened);
    }
    
    @Test
    public void testEventSerializerRoundTripCreate() {
        Serializer s = createMicrowaveEventSerializer();
        assertTrue(s.deserialize(s.serialize(new Create())) instanceof Create);
    }

    @Test
    public void test() throws IOException {
        File directory = File.createTempFile("db-", "", new File("target"));
        directory.mkdir();
        Serializer entitySerializer = createMicrowaveSerializer();
        Serializer eventSerializer = createMicrowaveEventSerializer();
        MicrowaveBehaviour<String> behaviour = createMicrowaveBehaviour();
        Function<Class<?>, EntityBehaviour<?, String>> behaviourFactory = cls -> behaviour;
        PersistenceH2 p = new PersistenceH2(directory, Executors.newScheduledThreadPool(5), ClockDefault.instance(),
                entitySerializer, eventSerializer, behaviourFactory);
        p.create();
        p.initialize();
        
    }

    private static MicrowaveBehaviour<String> createMicrowaveBehaviour() {
        MicrowaveBehaviour<String> behaviour = new MicrowaveBehaviourBase<String>() {

            @Override
            public MicrowaveStateMachine<String> create(String id) {
                return MicrowaveStateMachine.create(Microwave.fromId(id), id, this,
                        MicrowaveStateMachine.State.READY_TO_COOK);
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

            @Override
            public Object deserialize(byte[] bytes) {
                String className = new String(bytes, UTF_8);
                try {
                    return Class.forName(className).newInstance();
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

            @Override
            public Object deserialize(byte[] bytes) {
                int serialNumber = Integer.parseInt(new String(bytes, UTF_8));
                return new Microwave(serialNumber);
            }
        };
        return entitySerializer;
    }

}
