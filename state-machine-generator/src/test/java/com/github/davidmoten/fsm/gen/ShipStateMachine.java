package com.github.davidmoten.fsm.gen;

import com.github.davidmoten.fsm.StateMachineTest.In;
import com.github.davidmoten.fsm.runtime.Create;
import com.github.davidmoten.guavamini.Preconditions;
import com.github.davidmoten.fsm.runtime.Event;
import com.github.davidmoten.fsm.Ship;
import com.github.davidmoten.fsm.StateMachineTest.Risky;
import com.github.davidmoten.fsm.gen.ShipBehaviour;
import com.github.davidmoten.fsm.StateMachineTest.Out;

public final class ShipStateMachine {

    private final Ship ship;
    private final ShipBehaviour behaviour;
    private final State state;

    private ShipStateMachine(Ship ship, ShipBehaviour behaviour, State state) {
        Preconditions.checkNotNull(ship, "ship cannot be null");
        Preconditions.checkNotNull(behaviour, "behaviour cannot be null");
        Preconditions.checkNotNull(state, "state cannot be null");
        this.ship = ship;
        this.behaviour = behaviour;
        this.state = state;
    }

    public static ShipStateMachine create(Ship ship, ShipBehaviour behaviour) {
        return new ShipStateMachine(ship, behaviour, State.INITIAL);
    }

    private static enum State {
        INITIAL,
        NEVER_OUTSIDE,
        OUTSIDE,
        INSIDE_NOT_RISKY,
        INSIDE_RISKY;
    }

    public ShipStateMachine event(Create event) {
        return _event(event);
    }

    public ShipStateMachine event(Out event) {
        return _event(event);
    }

    public ShipStateMachine event(In event) {
        return _event(event);
    }

    public ShipStateMachine event(Risky event) {
        return _event(event);
    }

    private ShipStateMachine _event(Event<?> event) {
        Preconditions.checkNotNull(event);
        if (state == State.INITIAL && event instanceof Create) {
            State nextState = State.NEVER_OUTSIDE;
            Ship nextObject = behaviour.onEntry_NeverOutside((Create) event);
            return new ShipStateMachine(nextObject, behaviour, nextState);
        } else if (state == State.NEVER_OUTSIDE && event instanceof Out) {
            State nextState = State.OUTSIDE;
            Ship nextObject = behaviour.onEntry_Outside(ship, (Out) event);
            return new ShipStateMachine(nextObject, behaviour, nextState);
        } else if (state == State.OUTSIDE && event instanceof In) {
            State nextState = State.INSIDE_NOT_RISKY;
            Ship nextObject = behaviour.onEntry_InsideNotRisky(ship, (In) event);
            return new ShipStateMachine(nextObject, behaviour, nextState);
        } else if (state == State.INSIDE_NOT_RISKY && event instanceof Risky) {
            State nextState = State.INSIDE_RISKY;
            Ship nextObject = behaviour.onEntry_InsideRisky(ship, (Risky) event);
            return new ShipStateMachine(nextObject, behaviour, nextState);
        }
        return this;
    }
}