package com.github.davidmoten.fsm.gen;

import com.github.davidmoten.fsm.Ship;
import com.github.davidmoten.fsm.StateMachineTest.Out;
import com.github.davidmoten.fsm.model.Created;
import com.github.davidmoten.fsm.model.Event;
import com.github.davidmoten.guavamini.Preconditions;

public class ShipStateMachine {

    private final ShipBehaviour behaviour;
    private final State state;
    private final Ship object;

    private enum State {
        CREATED, NEVER_OUTSIDE, OUTSIDE, INSIDE_NOT_RISKY;
    }

    public ShipStateMachine(Ship object, ShipBehaviour behaviour) {
        this(object, behaviour, State.CREATED);
    }

    private ShipStateMachine(Ship object, ShipBehaviour behaviour, State state) {
        Preconditions.checkNotNull(object);
        Preconditions.checkNotNull(behaviour);
        Preconditions.checkNotNull(state);
        this.object = object;
        this.behaviour = behaviour;
        this.state = state;
    }

    public synchronized ShipStateMachine event(Event<?> event) {
        Preconditions.checkNotNull(event);
        if (state == State.CREATED && event instanceof Created) {
            State nextState = State.NEVER_OUTSIDE;
            Ship nextObject = behaviour.onEntry_NeverOutside(object, (Created) event);
            return new ShipStateMachine(nextObject, behaviour, nextState);
        } else if (state == State.NEVER_OUTSIDE && event instanceof Out) {
            State nextState = State.OUTSIDE;
            Ship nextObject = behaviour.onEntry_Outside(object, (Out) event);
            return new ShipStateMachine(nextObject, behaviour, nextState);
        } else {
            return this;
        }
    }

}
