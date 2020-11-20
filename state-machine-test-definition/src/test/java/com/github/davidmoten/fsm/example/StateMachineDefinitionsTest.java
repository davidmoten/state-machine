package com.github.davidmoten.fsm.example;

import org.junit.Test;

import com.github.davidmoten.fsm.graph.NodeOptions;
import com.github.davidmoten.fsm.model.StateMachineDefinition;

public class StateMachineDefinitionsTest {

    @Test
    public void testPlantUml() {

        for (StateMachineDefinition<?> s: new StateMachineDefinitions().get()) {
            System.out.println(s.plantuml(g -> NodeOptions.defaultInstance(), true));
        }
    }

}
