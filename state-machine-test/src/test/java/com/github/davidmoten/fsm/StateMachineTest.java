package com.github.davidmoten.fsm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.github.davidmoten.fsm.example.generated.MicrowaveBehaviour;
import com.github.davidmoten.fsm.example.generated.MicrowaveBehaviourBase;
import com.github.davidmoten.fsm.example.generated.MicrowaveStateMachine;
import com.github.davidmoten.fsm.example.generated.ShipBehaviour;
import com.github.davidmoten.fsm.example.generated.ShipBehaviourBase;
import com.github.davidmoten.fsm.example.generated.ShipStateMachine;
import com.github.davidmoten.fsm.example.microwave.ButtonPressed;
import com.github.davidmoten.fsm.example.microwave.DoorOpened;
import com.github.davidmoten.fsm.example.microwave.Microwave;
import com.github.davidmoten.fsm.example.microwave.TimerTimesOut;
import com.github.davidmoten.fsm.example.ship.In;
import com.github.davidmoten.fsm.example.ship.Out;
import com.github.davidmoten.fsm.example.ship.Ship;
import com.github.davidmoten.fsm.runtime.Clock;
import com.github.davidmoten.fsm.runtime.Create;
import com.github.davidmoten.fsm.runtime.EntityStateMachine;
import com.github.davidmoten.fsm.runtime.Signaller;
import com.github.davidmoten.fsm.runtime.rx.IdMapper;
import com.github.davidmoten.fsm.runtime.rx.Processor;
import com.github.davidmoten.fsm.runtime.rx.StateMachineFactory;

import rx.functions.Func2;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.schedulers.TestScheduler;

public class StateMachineTest {

    @Test
    public void testShipRuntime() {
        final Ship ship = new Ship("12345", "6789", 35.0f, 141.3f);
        List<Integer> list = new ArrayList<>();
        ShipBehaviour shipBehaviour = new ShipBehaviourBase() {

            @Override
            public Ship onEntry_Outside(Signaller signaller, Ship ship, Out out) {
                list.add(1);
                return new Ship(ship.imo(), ship.mmsi(), out.lat, out.lon);
            }

            @Override
            public Ship onEntry_NeverOutside(Signaller signaller, Create created) {
                list.add(2);
                return ship;
            }

            @Override
            public Ship onEntry_InsideNotRisky(Signaller signaller, Ship ship, In in) {
                list.add(3);
                return new Ship(ship.imo(), ship.mmsi(), in.lat, in.lon);
            }

        };
        ShipStateMachine.create(shipBehaviour)
                //
                .signal(Create.instance())
                //
                .signal(new In(1.0f, 2.0f))
                //
                .signal(new Out(1.0f, 3.0f));

        assertEquals(Arrays.asList(2, 1), list);
    }

    @Test
    public void testMicrowaveRuntime() {
        Microwave microwave = new Microwave("1");
        MicrowaveBehaviour behaviour = new MicrowaveBehaviourBase();
        MicrowaveStateMachine m = MicrowaveStateMachine.create(microwave, behaviour,
                MicrowaveStateMachine.State.READY_TO_COOK);
        m.signal(new ButtonPressed()).signal(new DoorOpened());
    }

    @Test
    public void testMicrowaveProcessor() throws InterruptedException {
        TestScheduler scheduler = new TestScheduler();
        Microwave microwave = new Microwave("1");
        MicrowaveBehaviour behaviour = new MicrowaveBehaviourBase() {
            @Override
            public Microwave onEntry_Cooking(Signaller signaller, Microwave microwave,
                    ButtonPressed event) {
                signaller.signalToSelf(new TimerTimesOut(), 30, TimeUnit.SECONDS);
                return microwave;
            }
        };

        // this is where we define how instances are instantiated and looked up
        IdMapper<String> idMapper = IdMapper.cls(Microwave.class).hasMapper(x -> x.id()).build();

        Func2<Class<?>, String, EntityStateMachine<?>> stateMachineFactory = StateMachineFactory
                .cls(Microwave.class)
                .<String> hasFactory(
                        id -> MicrowaveStateMachine.create(new Microwave(id), behaviour,
                                MicrowaveStateMachine.State.READY_TO_COOK, Clock.from(scheduler)))
                .build();

        Processor<String> processor = Processor.idMapper(idMapper)
                .stateMachineFactory(stateMachineFactory)
                .processingScheduler(Schedulers.immediate()).signalScheduler(scheduler).build();

        TestSubscriber<EntityStateMachine<?>> ts = TestSubscriber.create();
        processor.observable().doOnNext(m -> System.out.println(m.state())).subscribe(ts);
        processor.signal(microwave, new ButtonPressed());
        ts.assertValueCount(1);
        assertEquals(MicrowaveStateMachine.State.COOKING, ts.getOnNextEvents().get(0).state());
        scheduler.advanceTimeBy(29, TimeUnit.SECONDS);
        ts.assertValueCount(1);
        scheduler.advanceTimeBy(1, TimeUnit.SECONDS);

        ts.assertNoErrors();
        ts.assertValueCount(2);
        {
            List<EntityStateMachine<?>> list = ts.getOnNextEvents();
            assertEquals(MicrowaveStateMachine.State.COOKING, list.get(0).state());
            assertEquals(MicrowaveStateMachine.State.COOKING_COMPLETE, list.get(1).state());
        }
        processor.signal(microwave, new DoorOpened());
        ts.assertValueCount(3);
        assertEquals(MicrowaveStateMachine.State.DOOR_OPEN, ts.getOnNextEvents().get(2).state());

        processor.signal(microwave, new ButtonPressed());
        {
            // should not be a transition
            ts.assertValueCount(4);
            List<EntityStateMachine<?>> list = ts.getOnNextEvents();
            assertEquals(MicrowaveStateMachine.State.DOOR_OPEN, list.get(3).state());
            assertFalse(list.get(3).transitionOccurred());
        }

        processor.onCompleted();
        ts.awaitTerminalEvent();
    }

}
