package com.github.davidmoten.fsm.runtime.rx;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.davidmoten.fsm.runtime.EntityStateMachine;
import com.github.davidmoten.fsm.runtime.ObjectState;
import com.github.davidmoten.fsm.runtime.Signal;
import com.github.davidmoten.rx.Transformers;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.subjects.PublishSubject;

public class Processor<Id> {

    private final PublishSubject<Signal<?, ?>> subject;
    private final Func1<Object, Id> id;
    private final Func1<Id, EntityStateMachine<?>> stateMachineCreator;
    private final Map<Id, EntityStateMachine<?>> stateMachines = new ConcurrentHashMap<>();

    public Processor(Func1<Object, Id> id, Func1<Id, EntityStateMachine<?>> stateMachineCreator) {
        this.subject = PublishSubject.create();
        this.id = id;
        this.stateMachineCreator = stateMachineCreator;
    }

    public Observable<EntityStateMachine<?>> asObservable() {
        return subject
                //
                .toSerialized()
                //
                .groupBy(id)
                //
                // .flatMap(g -> g.scan(stateMachineCreator.call(g.getKey()),
                // (m, signal) -> m.event(signal.event()))
                // .doOnNext(m -> stateMachines.put(g.getKey(), m)));
                .flatMap(g -> g.flatMap(x -> process(g.getKey(), x)));
    }

    private Observable<EntityStateMachine<?>> process(Id id, Signal<?, ?> x) {

        Func0<Deque<Signal<?, ?>>> initialStateFactory = () -> new ArrayDeque<>();
        Func3<Deque<Signal<?, ?>>, Signal<?, ?>, Subscriber<EntityStateMachine<?>>, Deque<Signal<?, ?>>> transition = (
                q, signal, subscriber) -> {
            EntityStateMachine<?> m = stateMachines.get(id).event(x.event());
            subscriber.onNext(m);
            return null;
        };
        Func2<Deque<Signal<?, ?>>, Subscriber<EntityStateMachine<?>>, Boolean> completion = (q,
                sub) -> true;
        return Observable.just(x)
                .compose(Transformers.stateMachine(initialStateFactory, transition, completion));
    }

    public void signal(Signal<?, ?> signal) {
        subject.onNext(signal);
    }

    @SuppressWarnings("unchecked")
    public <T> ObjectState<T> get(Id id) {
        return (EntityStateMachine<T>) stateMachines.get(id);
    }

}
