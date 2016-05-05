package com.github.davidmoten.fsm.example;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;

import com.github.davidmoten.fsm.example.ship.In;
import com.github.davidmoten.fsm.example.ship.Out;
import com.github.davidmoten.fsm.example.ship.Risky;
import com.github.davidmoten.fsm.example.ship.Ship;
import com.github.davidmoten.fsm.model.State;
import com.github.davidmoten.fsm.model.StateMachine;
import com.github.davidmoten.fsm.runtime.Create;

public class GenerateTest {

    @Test
    public void test() throws IOException {
        File directory = new File("target/test-gen/java");
        String pkg = "com.github.davidmoten.fsm.generated";

        StateMachine<Ship> m = StateMachine.create(Ship.class);

        // create states (with the event used to transition to it)
        State<Ship, Create> neverOutside = m.createState("Never Outside", Create.class);
        State<Ship, Out> outside = m.createState("Outside", Out.class);
        State<Ship, In> insideNotRisky = m.createState("Inside Not Risky", In.class);
        State<Ship, Risky> insideRisky = m.createState("Inside Risky", Risky.class);

        // create transitions and generate classes
        neverOutside.initial().to(outside).to(insideNotRisky).to(insideRisky)
                .generateClasses(directory, pkg);

        String pkgPath = pkg.replace(".", File.separator) + File.separator;
        File stateMachineFile = new File(directory, pkgPath + "ShipStateMachine.java");
        println(stateMachineFile);
        File behaviourFile = new File(directory, pkgPath + "ShipBehaviour.java");
        println(behaviourFile);
        File behaviourBaseFile = new File(directory, pkgPath + "ShipBehaviourBase.java");
        println(behaviourBaseFile);
        System.out.println(m.documentationHtml());
    }

    private static void println(File file) throws IOException {
        System.out.println(new String(Files.readAllBytes(file.toPath())));
    }

}
