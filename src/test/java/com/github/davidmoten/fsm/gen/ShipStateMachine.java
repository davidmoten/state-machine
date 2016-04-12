package com.github.davidmoten.fsm.gen;

import com.github.davidmoten.fsm.Ship;
import com.github.davidmoten.fsm.StateMachineTest.In;
import com.github.davidmoten.fsm.StateMachineTest.Out;
import com.github.davidmoten.fsm.model.Event;
import com.github.davidmoten.fsm.runtime.Create;
import com.github.davidmoten.guavamini.Preconditions;

public class ShipStateMachine {

    private final Ship ship;
    private final ShipBehaviour behaviour;
    private final State state;

    private ShipStateMachine(Ship ship, ShipBehaviour behaviour, State state) {
        Preconditions.checkNotNull(ship);
        Preconditions.checkNotNull(behaviour);
        Preconditions.checkNotNull(state);
        this.ship = ship;
        this.behaviour = behaviour;
        this.state = state;
    }

    public static ShipStateMachine create(Ship ship, ShipBehaviour behaviour) {
        return new ShipStateMachine(ship, behaviour, State.CREATED);
    }

    private enum State {
        CREATED, NEVER_OUTSIDE, OUTSIDE, INSIDE_NOT_RISKY;
    }

    public ShipStateMachine event(Create event) {
        return _event(event);
    }

    public ShipStateMachine event(Out event) {
        return _event(event);
    }

    private ShipStateMachine _event(Event<?> event) {
        Preconditions.checkNotNull(event);
        if (state == State.CREATED && event instanceof Create) {
            State nextState = State.NEVER_OUTSIDE;
            Ship nextObject = behaviour.onEntry_NeverOutside(ship, (Create) event);
            return new ShipStateMachine(nextObject, behaviour, nextState);
        } else if (state == State.NEVER_OUTSIDE && event instanceof Out) {
            State nextState = State.OUTSIDE;
            Ship nextObject = behaviour.onEntry_Outside(ship, (Out) event);
            return new ShipStateMachine(nextObject, behaviour, nextState);
        } else {
            return this;
        }
    }

    public ShipStateMachine event(In event) {
        return _event(event);
    }

}
