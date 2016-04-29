package com.github.davidmoten.fsm;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.github.davidmoten.fsm.example.generated.MicrowaveBehaviour;
import com.github.davidmoten.fsm.example.generated.MicrowaveBehaviourBase;
import com.github.davidmoten.fsm.example.generated.MicrowaveStateMachine;
import com.github.davidmoten.fsm.example.generated.ShipBehaviour;
import com.github.davidmoten.fsm.example.generated.ShipBehaviourBase;
import com.github.davidmoten.fsm.example.generated.ShipStateMachine;
import com.github.davidmoten.fsm.example.microwave.ButtonPressed;
import com.github.davidmoten.fsm.example.microwave.DoorOpened;
import com.github.davidmoten.fsm.example.microwave.Microwave;
import com.github.davidmoten.fsm.example.ship.In;
import com.github.davidmoten.fsm.example.ship.Out;
import com.github.davidmoten.fsm.example.ship.Ship;
import com.github.davidmoten.fsm.runtime.Create;
import com.github.davidmoten.fsm.runtime.Signaller;

public class StateMachineTest {

    @Test
    public void testShipRuntime() {
        final Ship ship = new Ship("12345", "6789", 35.0f, 141.3f);
        List<Integer> list = new ArrayList<>();
        ShipBehaviour shipBehaviour = new ShipBehaviourBase() {

            @Override
            public Ship onEntry_Outside(Signaller signaller, Ship ship, Object id, Out out) {
                list.add(1);
                return new Ship(ship.imo(), ship.mmsi(), out.lat, out.lon);
            }

            @Override
            public Ship onEntry_NeverOutside(Signaller signaller, Object id, Create created) {
                list.add(2);
                return ship;
            }

            @Override
            public Ship onEntry_InsideNotRisky(Signaller signaller, Ship ship, Object id, In in) {
                list.add(3);
                return new Ship(ship.imo(), ship.mmsi(), in.lat, in.lon);
            }

        };
        ShipStateMachine.create("12345", shipBehaviour)
                //
                .signal(Create.instance())
                //
                .signal(new In(1.0f, 2.0f))
                //
                .signal(new Out(1.0f, 3.0f));

        assertEquals(Arrays.asList(2, 1), list);
    }

    @Test
    public void testMicrowaveRuntime() {
        Microwave microwave = new Microwave("1");
        MicrowaveBehaviour behaviour = new MicrowaveBehaviourBase();
        MicrowaveStateMachine m = MicrowaveStateMachine.create(microwave, "1", behaviour,
                MicrowaveStateMachine.State.READY_TO_COOK);
        m.signal(new ButtonPressed()).signal(new DoorOpened());
    }

}
