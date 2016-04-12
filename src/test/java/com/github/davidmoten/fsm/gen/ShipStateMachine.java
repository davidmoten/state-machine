package com.github.davidmoten.fsm.gen;

import com.github.davidmoten.fsm.Ship;
import com.github.davidmoten.fsm.StateMachineTest.Out;
import com.github.davidmoten.fsm.model.Created;
import com.github.davidmoten.fsm.model.Event;
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

    public synchronized ShipStateMachine event(Event<?> event) {
        Preconditions.checkNotNull(event);
        if (state == State.CREATED && event instanceof Created) {
            State nextState = State.NEVER_OUTSIDE;
            Ship nextObject = behaviour.onEntry_NeverOutside(ship, (Created) event);
            return new ShipStateMachine(nextObject, behaviour, nextState);
        } else if (state == State.NEVER_OUTSIDE && event instanceof Out) {
            State nextState = State.OUTSIDE;
            Ship nextObject = behaviour.onEntry_Outside(ship, (Out) event);
            return new ShipStateMachine(nextObject, behaviour, nextState);
        } else {
            return this;
        }
    }

}
