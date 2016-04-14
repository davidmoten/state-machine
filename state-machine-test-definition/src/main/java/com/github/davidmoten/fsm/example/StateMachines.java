package com.github.davidmoten.fsm.example;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.github.davidmoten.fsm.example.microwave.ButtonPressed;
import com.github.davidmoten.fsm.example.microwave.DoorClosed;
import com.github.davidmoten.fsm.example.microwave.DoorOpened;
import com.github.davidmoten.fsm.example.microwave.Microwave;
import com.github.davidmoten.fsm.example.microwave.TimerTimesOut;
import com.github.davidmoten.fsm.example.ship.In;
import com.github.davidmoten.fsm.example.ship.NextPort;
import com.github.davidmoten.fsm.example.ship.NextPortUnknown;
import com.github.davidmoten.fsm.example.ship.NotRisky;
import com.github.davidmoten.fsm.example.ship.Out;
import com.github.davidmoten.fsm.example.ship.Risky;
import com.github.davidmoten.fsm.example.ship.Ship;
import com.github.davidmoten.fsm.model.State;
import com.github.davidmoten.fsm.model.StateMachine;
import com.github.davidmoten.fsm.runtime.Create;

public class StateMachines implements Supplier<List<StateMachine<?>>> {

    @Override
    public List<StateMachine<?>> get() {
        return Arrays.asList(createShipStateMachine(), createMicrowaveStateMachine());

    }

    private static StateMachine<Ship> createShipStateMachine() {
        StateMachine<Ship> m = StateMachine.create(Ship.class);

        // create states (with the event used to transition to it)
        State<Void> neverOutside = m.state("Never Outside", Create.class);
        State<Out> outside = m.state("Outside", Out.class);
        State<In> insideNotRisky = m.state("Inside Not Risky", In.class);
        State<Risky> insideRisky = m.state("Inside Risky", Risky.class);
        State<NotRisky> insideNotRiskyAlreadyNotified = m.state("Inside Not Risky Already Notified",
                NotRisky.class);
        State<Out> departed = m.state("Departed", Out.class);
        State<NextPort> departedToPort = m.state("Departed To Port", NextPort.class);
        State<NextPortUnknown> departedToUnknownPort = m.state("Departed To Unknown Port",
                NextPortUnknown.class);
        State<Risky> notifiedNextPort = m.state("Notified Next Port", Risky.class);
        State<NotRisky> departedToPortNotRisky = m.state("Departed To Port Not Risky",
                NotRisky.class);

        // create transitions and generate classes
        neverOutside.initial()
                //
                .to(outside.from(notifiedNextPort).from(departedToPortNotRisky.from(departedToPort))
                        .from(departedToUnknownPort.from(departed)))
                .to(insideNotRisky)
                .to(insideRisky.from(insideRisky).from(insideNotRiskyAlreadyNotified))
                .to(departed.from(insideNotRisky).from(insideNotRiskyAlreadyNotified))
                .to(departedToPort).to(notifiedNextPort);
        return m;
    }

    private static StateMachine<Microwave> createMicrowaveStateMachine() {
        StateMachine<Microwave> m = StateMachine.create(Microwave.class);
        State<DoorClosed> readyToCook = m.state("Ready to Cook", DoorClosed.class);
        State<DoorOpened> doorOpen = m.state("Door Open", DoorOpened.class);
        State<ButtonPressed> cooking = m.state("Cooking", ButtonPressed.class);
        State<DoorOpened> cookingInterruped = m.state("Cooking Interrupted", DoorOpened.class);
        State<TimerTimesOut> cookingComplete = m.state("Cooking Complete", TimerTimesOut.class);

        readyToCook.to(cooking).to(cookingInterruped).to(
                readyToCook.from(doorOpen.from(readyToCook).from(cookingComplete.from(cooking))));

        return m;
    }

}
