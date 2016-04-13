package com.github.davidmoten.fsm.example.generated;

import com.github.davidmoten.fsm.example.In;
import com.github.davidmoten.fsm.example.Out;
import com.github.davidmoten.fsm.example.Risky;
import com.github.davidmoten.fsm.example.Ship;
import com.github.davidmoten.fsm.example.generated.ShipBehaviour;

import java.lang.Override;

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
