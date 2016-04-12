package com.github.davidmoten.fsm;

import java.io.File;

import org.junit.Test;

import com.github.davidmoten.fsm.model.CreateEvent;
import com.github.davidmoten.fsm.model.Event;
import com.github.davidmoten.fsm.model.State;
import com.github.davidmoten.fsm.model.StateMachine;

public class StateMachineTest {

    @Test
    public void test() {
        StateMachine m = StateMachine.create("Ship");

        // create states
        State<Void> neverOutside = m.state("Never Outside", CreateEvent.class);
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
