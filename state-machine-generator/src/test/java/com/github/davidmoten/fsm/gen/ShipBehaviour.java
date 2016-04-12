package com.github.davidmoten.fsm.gen;

import com.github.davidmoten.fsm.Ship;
import com.github.davidmoten.fsm.StateMachineTest.In;
import com.github.davidmoten.fsm.StateMachineTest.Out;
import com.github.davidmoten.fsm.StateMachineTest.Risky;
import com.github.davidmoten.fsm.runtime.Create;
import com.github.davidmoten.fsm.runtime.EventVoid;

public interface ShipBehaviour {

    Ship onEntry_Initial(Ship ship, EventVoid event);

    Ship onEntry_NeverOutside(Ship ship, Create event);

    Ship onEntry_Outside(Ship ship, Out event);

    Ship onEntry_InsideNotRisky(Ship ship, In event);

    Ship onEntry_InsideRisky(Ship ship, Risky event);

}
