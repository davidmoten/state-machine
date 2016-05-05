package com.github.davidmoten.fsm;

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
        Microwave microwave = new Microwave("1");
        MicrowaveBehaviour behaviour = new MicrowaveBehaviourBase();
        MicrowaveStateMachine m = MicrowaveStateMachine.create(microwave, "1", behaviour,
                MicrowaveStateMachine.State.READY_TO_COOK);
        m.signal(new ButtonPressed()).signal(new DoorOpened());
    }

}
