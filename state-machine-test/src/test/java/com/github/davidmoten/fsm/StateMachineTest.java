package com.github.davidmoten.fsm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.github.davidmoten.fsm.example.generated.MicrowaveBehaviour;
import com.github.davidmoten.fsm.example.generated.MicrowaveBehaviourBase;
import com.github.davidmoten.fsm.example.generated.MicrowaveStateMachine;
import com.github.davidmoten.fsm.example.microwave.ButtonPressed;
import com.github.davidmoten.fsm.example.microwave.DoorOpened;
import com.github.davidmoten.fsm.example.microwave.Microwave;

public class StateMachineTest {

    @Test
    public void testMicrowaveRuntime() {
        Microwave microwave = new Microwave(1);
        MicrowaveBehaviour<String> behaviour = new MicrowaveBehaviourBase<String>();
        MicrowaveStateMachine<String> m = MicrowaveStateMachine.create(microwave,
                Microwave.idFromSerialNumber(1), behaviour,
                MicrowaveStateMachine.State.READY_TO_COOK);
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
