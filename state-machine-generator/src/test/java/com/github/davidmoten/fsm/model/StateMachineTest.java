package com.github.davidmoten.fsm.model;

import org.junit.Test;

import com.github.davidmoten.fsm.graph.NodeOptions;
import com.github.davidmoten.fsm.runtime.Event;

public class StateMachineTest {

    @Test
    public void testPlantUmlGeneration() {

        StateMachineDefinition<Person> m = StateMachineDefinition.create(Person.class);
        State<Person, Illness> sick = m.createState("Sick").event(Illness.class).documentation("has an illness");
        State<Person, Health> well = m.createState("Well").event(Health.class);
        State<Person, Dies> dead = m.createState("Dead").event(Dies.class).documentation("not alive\nready for burial");
        
        m.addInitialTransition(well);
        well.from(sick).to(sick).to(dead);
        System.out.println(m.plantuml(g -> NodeOptions.defaultInstance(), true));
    }

    private static final class Person {

    }

    private static final class Illness implements Event<Person> {

    }

    private static final class Health implements Event<Person> {

    }

    private static final class Dies implements Event<Person> {

    }

}
