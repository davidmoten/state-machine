package com.github.davidmoten.fsm.gen;

import com.github.davidmoten.fsm.Ship;
import com.github.davidmoten.fsm.StateMachineTest.In;
import com.github.davidmoten.fsm.StateMachineTest.Out;
import com.github.davidmoten.fsm.runtime.Create;

public interface ShipBehaviour {

    Ship onEntry_NeverOutside(Ship ship, Create created);

    Ship onEntry_Outside(Ship ship, Out out);

    Ship onEntry_InsideNotRisky(Ship ship, In in);
}
