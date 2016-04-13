package com.github.davidmoten.fsm.example.generated;

import com.github.davidmoten.fsm.example.In;
import com.github.davidmoten.fsm.example.Out;
import com.github.davidmoten.fsm.example.Risky;
import com.github.davidmoten.fsm.example.Ship;
import com.github.davidmoten.fsm.runtime.Create;

public interface ShipBehaviour {

    Ship onEntry_NeverOutside(Create event);

    Ship onEntry_Outside(Ship ship, Out event);

    Ship onEntry_InsideNotRisky(Ship ship, In event);

    Ship onEntry_InsideRisky(Ship ship, Risky event);
}
