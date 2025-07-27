package com.github.davidmoten.fsm.rx;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.davidmoten.fsm.example.generated.MicrowaveBehaviour;
import com.github.davidmoten.fsm.example.generated.MicrowaveBehaviourBase;
import com.github.davidmoten.fsm.example.generated.MicrowaveStateMachine;
import com.github.davidmoten.fsm.example.microwave.Microwave;
import com.github.davidmoten.fsm.example.microwave.event.ButtonPressed;
import com.github.davidmoten.fsm.example.microwave.event.DoorOpened;
import com.github.davidmoten.fsm.example.microwave.event.TimerTimesOut;
import com.github.davidmoten.fsm.runtime.Signal;
import com.github.davidmoten.fsm.runtime.Signaller;
import com.github.davidmoten.fsm.runtime.rx.Processor;

import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;
import io.reactivex.rxjava3.subscribers.TestSubscriber;

public class StreamingTest {

    @Test
    public void testJsonInputToStateMachineIssue1() throws InterruptedException {

        // JSON source stream (could contain other messages about other
        // Microwaves with different ids which will be processed concurrently by
        // the Processor)
        Flowable<String> messages = Flowable.just(
                "{\"cls\": \"Microwave\", \"id\": \"1\",\"event\": \"ButtonPressed\"}",
                "{\"cls\": \"Microwave\", \"id\": \"1\",\"event\": \"DoorOpened\"}",
                "{\"cls\": \"Microwave\", \"id\": \"1\",\"event\": \"ButtonPressed\"}");

        Flowable<Signal<?, String>> signals = messages //
                .map(msg -> toSignal(msg));

        // special scheduler that we will use to schedule signals and to process
        // events
        Scheduler scheduler = Schedulers.from(Executors.newFixedThreadPool(3));

        // create the signal processor
        Processor<String> processor = createProcessor(scheduler, signals);

        // using a test subscriber because has easy assert methods on it
        TestSubscriber<Object> ts = TestSubscriber.create();

        // subscribe to the stream of entity states that is produced from the
        // signals stream
        processor //
                .flowable() //
                // just output the states
                .map(esm -> esm.state()) //
                .subscribe(ts);

        // wait for processing to finish (is running asynchronously using the
        // scheduler)
        Thread.sleep(1000);

        // assert that things happened as we expected
        ts.assertValues( //
                MicrowaveStateMachine.State.COOKING,
                MicrowaveStateMachine.State.COOKING_INTERRUPTED,
                MicrowaveStateMachine.State.COOKING_INTERRUPTED);

    }

    private static Signal<?, String> toSignal(String msg) {
        JsonNode t = readTree(msg);
        String id = t.get("id").asText();
        String event = t.get("event").asText();
        if ("ButtonPressed".equals(event)) {
            return Signal.create(Microwave.class, id, new ButtonPressed());
        } else if ("DoorOpened".equals(event)) {
            return Signal.create(Microwave.class, id, new DoorOpened());
        } else
            throw new RuntimeException("event not recognized: " + event);
    }

    private static JsonNode readTree(String s) {
        ObjectMapper m = new ObjectMapper();
        try {
            return m.readTree(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Processor<String> createProcessor(Scheduler scheduler,
            Flowable<Signal<?, String>> signals) {
        MicrowaveBehaviour<String> behaviour = createMicrowaveBehaviour();

        // build a processor
        return Processor //
                .behaviour(Microwave.class, behaviour) //
                .processingScheduler(scheduler) //
                .signalScheduler(scheduler) //
                .signals(signals) //
                .preTransition((m, event, state) -> System.out.println("[preTransition] "
                        + event.getClass().getSimpleName() + ": " + m.state() + " -> " + state)) //
                .postTransition(m -> System.out.println("[postTransition] " + m.state())) //
                .build();
    }

    private static MicrowaveBehaviourBase<String> createMicrowaveBehaviour() {
        return new MicrowaveBehaviourBase<String>() {

            @Override
            public MicrowaveStateMachine<String> create(String id) {
                return MicrowaveStateMachine.create(Microwave.fromId(id), id, this,
                        MicrowaveStateMachine.State.READY_TO_COOK);
            }

            @Override
            public Microwave onEntry_Cooking(Signaller<Microwave, String> signaller,
                    Microwave microwave, String id, ButtonPressed event, boolean isReplay) {
                signaller.signalToSelf(new TimerTimesOut(), 30, TimeUnit.SECONDS);
                return microwave;
            }

        };
    }
}