package com.github.davidmoten.fsm.example;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.github.davidmoten.fsm.example.microwave.ButtonPressed;
import com.github.davidmoten.fsm.example.microwave.DoorClosed;
import com.github.davidmoten.fsm.example.microwave.DoorOpened;
import com.github.davidmoten.fsm.example.microwave.Microwave;
import com.github.davidmoten.fsm.example.microwave.TimerTimesOut;
import com.github.davidmoten.fsm.model.State;
import com.github.davidmoten.fsm.model.StateMachine;

public class StateMachines implements Supplier<List<StateMachine<?>>> {

    @Override
    public List<StateMachine<?>> get() {
        return Arrays.asList(createMicrowaveStateMachine());

    }

    private static StateMachine<Microwave> createMicrowaveStateMachine() {
        StateMachine<Microwave> m = StateMachine.create(Microwave.class);
        State<Microwave, DoorClosed> readyToCook = m.createState("Ready to Cook", DoorClosed.class)
                .documentation("<pre>entry/\nturn light off;</pre>");
        State<Microwave, DoorOpened> doorOpen = m.createState("Door Open", DoorOpened.class)
                .documentation("<pre>entry/\nturn light on;</pre>");
        State<Microwave, ButtonPressed> cooking = m.createState("Cooking", ButtonPressed.class)
                .documentation(
                        "<pre>entry/\nturn light on;\nsignal TimerTimesOut to self in 1 min;</pre>");
        State<Microwave, DoorOpened> cookingInterruped = m
                .createState("Cooking Interrupted", DoorOpened.class)
                .documentation("<pre>entry/\nturn light on;\ncancel signal to self;</pre>");
        State<Microwave, TimerTimesOut> cookingComplete = m
                .createState("Cooking Complete", TimerTimesOut.class)
                .documentation("<pre>entry/\nturn light off;</pre>");

        readyToCook.to(cooking).to(cookingInterruped).to(
                readyToCook.from(doorOpen.from(readyToCook).from(cookingComplete.from(cooking))));

        return m;
    }

}
