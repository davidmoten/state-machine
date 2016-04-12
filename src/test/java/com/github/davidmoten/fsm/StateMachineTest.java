package com.github.davidmoten.fsm;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.github.davidmoten.fsm.gen.ShipBehaviour;
import com.github.davidmoten.fsm.gen.ShipStateMachine;
import com.github.davidmoten.fsm.model.Created;
import com.github.davidmoten.fsm.model.Event;
import com.github.davidmoten.fsm.model.State;
import com.github.davidmoten.fsm.model.StateMachine;

public class StateMachineTest {

    @Test
    public void test() {
        StateMachine m = StateMachine.create("Ship");

        // create states
        State<Void> neverOutside = m.state("Never Outside", Created.class);
        State<Out> outside = m.state("Outside", Out.class);
        State<In> insideNotRisky = m.state("Inside Not Risky", In.class);
        State<Risky> insideRisky = m.state("Inside Risky", Risky.class);

        // create transitions
        neverOutside.to(outside);
        outside.to(insideNotRisky);
        insideNotRisky.to(insideRisky);

        m.generateClasses(new File("target/generated-sources/java"),
                "com.github.davidmoten.fsm.generated");
    }

    @Test
    public void testRuntime() {
        Ship ship = new Ship("12345", "6789", 35.0f, 141.3f);
        List<Integer> list = new ArrayList<>();
        ShipBehaviour shipBehaviour = new ShipBehaviour() {

            @Override
            public Ship onEntry_Outside(Ship ship, Out out) {
                list.add(1);
                return new Ship(ship.imo(), ship.mmsi(), out.lat, out.lon);
            }

            @Override
            public Ship onEntry_NeverOutside(Ship ship, Created created) {
                list.add(2);
                return new Ship(ship.imo(), ship.mmsi(), 0, 0);
            }

            @Override
            public Ship onEntry_InsideNotRisky(Ship ship, In in) {
                list.add(3);
                return new Ship(ship.imo(), ship.mmsi(), in.lat, in.lon);
            }
        };
        ShipStateMachine m = new ShipStateMachine(ship, shipBehaviour);
        m = m.event(Created.instance()).event(new In(1.0f, 2.0f)).event(new Out(1.0f, 3.0f));
        assertEquals(Arrays.asList(2, 1), list);
    }

    public static class In implements Event<In> {
        public final float lat;
        public final float lon;

        public In(float lat, float lon) {
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public In value() {
            return this;
        }
    }

    public static class Out implements Event<Out> {
        public final float lat;
        public final float lon;

        public Out(float lat, float lon) {
            this.lat = lat;
            this.lon = lon;
        }

        @Override
        public Out value() {
            return this;
        }
    }

    public static class Risky implements Event<Risky> {
        public final String message;

        public Risky(String message) {
            this.message = message;
        }

        @Override
        public Risky value() {
            return this;
        }

    }

}
