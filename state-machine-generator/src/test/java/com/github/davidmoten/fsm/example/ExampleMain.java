package com.github.davidmoten.fsm.example;

import java.io.File;

import com.github.davidmoten.fsm.model.State;
import com.github.davidmoten.fsm.model.StateMachine;
import com.github.davidmoten.fsm.runtime.Create;

public class ExampleMain {

    public static void main(String[] args) {
        File directory = new File(args[0]);
        String pkg = args[1];

        StateMachine<Ship> m = StateMachine.create(Ship.class);

        // create states (with the event used to transition to it)
        State<Void> neverOutside = m.state("Never Outside", Create.class);
        State<Out> outside = m.state("Outside", Out.class);
        State<In> insideNotRisky = m.state("Inside Not Risky", In.class);
        State<Risky> insideRisky = m.state("Inside Risky", Risky.class);

        // create transitions and generate classes
        neverOutside.initial().to(outside).to(insideNotRisky).to(insideRisky)
                .generateClasses(directory, pkg);
    }
}
