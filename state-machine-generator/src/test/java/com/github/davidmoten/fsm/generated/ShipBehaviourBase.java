package com.github.davidmoten.fsm.generated;

import com.github.davidmoten.fsm.StateMachineTest.In;
import com.github.davidmoten.fsm.Ship;
import java.lang.Override;
import com.github.davidmoten.fsm.StateMachineTest.Risky;
import com.github.davidmoten.fsm.generated.ShipBehaviour;
import com.github.davidmoten.fsm.StateMachineTest.Out;

public abstract class ShipBehaviourBase implements ShipBehaviour {

    @Override
    public Ship onEntry_Outside(Ship ship, Out event) {
        return ship;
    }

    @Override
    public Ship onEntry_InsideNotRisky(Ship ship, In event) {
        return ship;
    }

    @Override
    public Ship onEntry_InsideRisky(Ship ship, Risky event) {
        return ship;
    }

}