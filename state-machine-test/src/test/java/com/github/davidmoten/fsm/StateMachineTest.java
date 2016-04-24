package com.github.davidmoten.fsm;

import static org.junit.Assert.assertEquals;

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
import com.github.davidmoten.fsm.runtime.Create;
import com.github.davidmoten.fsm.runtime.EntityStateMachine;
import com.github.davidmoten.fsm.runtime.Signaller;
import com.github.davidmoten.fsm.runtime.rx.Processor;

import rx.functions.Func1;
import rx.observers.TestSubscriber;

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
		Microwave microwave = new Microwave("1");
		MicrowaveBehaviour behaviour = new MicrowaveBehaviourBase() {
			@Override
			public Microwave onEntry_Cooking(Signaller signaller, Microwave microwave, ButtonPressed event) {
				signaller.signalToSelf(new TimerTimesOut(), 500, TimeUnit.MILLISECONDS);
				return microwave;
			}
		};
		Func1<Object, String> identifier = x -> ((Microwave) x).id();
		Func1<String, EntityStateMachine<?>> stateMachineCreator = id -> MicrowaveStateMachine.create(new Microwave(id),
				behaviour, MicrowaveStateMachine.State.READY_TO_COOK);
		Processor<String> processor = Processor.create(identifier, stateMachineCreator);
		TestSubscriber<EntityStateMachine<?>> ts = TestSubscriber.create();
		processor.observable().doOnNext(m -> m.state()).subscribe(ts);
		processor.signal(microwave, new ButtonPressed());
		Thread.sleep(1000);
		processor.onCompleted();
		ts.awaitTerminalEvent();
		ts.assertNoErrors();
		ts.assertValueCount(1);
		System.out.println(ts.getOnNextEvents());
	}

}
