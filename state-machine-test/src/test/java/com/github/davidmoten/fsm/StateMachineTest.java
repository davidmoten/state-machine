package com.github.davidmoten.fsm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.github.davidmoten.fsm.example.generated.MicrowaveBehaviour;
import com.github.davidmoten.fsm.example.generated.MicrowaveBehaviourBase;
import com.github.davidmoten.fsm.example.generated.MicrowaveStateMachine;
import com.github.davidmoten.fsm.example.microwave.Microwave;
import com.github.davidmoten.fsm.example.microwave.event.ButtonPressed;
import com.github.davidmoten.fsm.example.microwave.event.DoorOpened;

public class StateMachineTest {

    @Test
    public void testMicrowaveRuntime() {
        MicrowaveBehaviour<String> behaviour = new MicrowaveBehaviourBase<String>() {

            @Override
            public MicrowaveStateMachine<String> create(String id) {
                return MicrowaveStateMachine.create(Microwave.fromId(id), id, this,
                        MicrowaveStateMachine.State.READY_TO_COOK);
            }
        };
        MicrowaveStateMachine<String> m = behaviour.create("1");
        assertFalse(m.transitionOccurred());
        assertFalse(m.previousState().isPresent());

        m = m.signal(new ButtonPressed());
        assertEquals(m.state(), MicrowaveStateMachine.State.COOKING);
        assertTrue(m.transitionOccurred());
        assertEquals(MicrowaveStateMachine.State.READY_TO_COOK, m.previousState().get());

        m = m.signal(new DoorOpened());
        assertEquals(MicrowaveStateMachine.State.COOKING, m.previousState().get());
        assertEquals(m.state(), MicrowaveStateMachine.State.COOKING_INTERRUPTED);
        assertTrue(m.transitionOccurred());

        // should have no effect because door is open
        m = m.signal(new ButtonPressed());
        assertFalse(m.transitionOccurred());
        assertEquals(m.state(), MicrowaveStateMachine.State.COOKING_INTERRUPTED);
        assertEquals(MicrowaveStateMachine.State.COOKING, m.previousState().get());
    }
    
}
