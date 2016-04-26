package com.github.davidmoten.fsm.runtime.rx;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.fsm.runtime.EntityStateMachine;
import com.github.davidmoten.fsm.runtime.Event;
import com.github.davidmoten.fsm.runtime.ObjectState;
import com.github.davidmoten.fsm.runtime.Signal;
import com.github.davidmoten.util.Preconditions;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observables.SyncOnSubscribe;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public final class Processor<Id> {

    private final Func1<Object, Id> idMapper;
    private final Func2<Class<?>, Id, EntityStateMachine<?>> stateMachineFactory;
    private final PublishSubject<Signal<?, ?>> subject;
    private final Map<ClassId<?>, EntityStateMachine<?>> stateMachines = new ConcurrentHashMap<>();
    private final Scheduler futureScheduler;
    private final Scheduler processingScheduler;

    private Processor(Func1<Object, Id> idMapper,
            Func2<Class<?>, Id, EntityStateMachine<?>> stateMachineFactory,
            Scheduler processingScheduler, Scheduler futureScheduler) {
        Preconditions.checkNotNull(idMapper);
        Preconditions.checkNotNull(stateMachineFactory);
        Preconditions.checkNotNull(futureScheduler);
        this.idMapper = idMapper;
        this.stateMachineFactory = stateMachineFactory;
        this.futureScheduler = futureScheduler;
        this.processingScheduler = processingScheduler;
        this.subject = PublishSubject.create();
    }

    public static <Id> Processor<Id> create(Func1<Object, Id> idMapper,
            Func2<Class<?>, Id, EntityStateMachine<?>> stateMachineFactory,
            Scheduler processingScheduler, Scheduler futureScheduler) {
        return new Processor<Id>(idMapper, stateMachineFactory, processingScheduler, futureScheduler);
    }

    public static <Id> Processor<Id> create(Func1<Object, Id> idMapper,
            Func2<Class<?>, Id, EntityStateMachine<?>> stateMachineFactory,
            Scheduler processingScheduler) {
        return new Processor<Id>(idMapper, stateMachineFactory, processingScheduler,
                Schedulers.computation());
    }

    public static <Id> Processor<Id> create(Func1<Object, Id> idMapper,
            Func2<Class<?>, Id, EntityStateMachine<?>> stateMachineFactory) {
        return new Processor<Id>(idMapper, stateMachineFactory, Schedulers.immediate(),
                Schedulers.computation());
    }

    public Observable<EntityStateMachine<?>> observable() {
        return Observable.defer(() -> {
            Worker worker = futureScheduler.createWorker();
            return subject
                    //
                    .toSerialized()
                    //
                    .doOnUnsubscribe(() -> worker.unsubscribe())
                    //
                    .groupBy(signal -> idMapper.call(signal.object()))
                    //
                    .flatMap(g -> g
                            //
                            .flatMap(x -> process(x.object().getClass(), g.getKey(), x.event(),
                                    worker))
                            //
                            .doOnNext(m -> stateMachines
                                    .put(new ClassId<Object>(m.cls(), g.getKey()), m))
                            //
                            .subscribeOn(processingScheduler));
        });
    }

    private static final class Signals {
        final Deque<Event<?>> signalsToSelf = new ArrayDeque<>();
        final Deque<Signal<?, ?>> signalsToOther = new ArrayDeque<>();
    }

    private Observable<EntityStateMachine<?>> process(Class<?> cls, Id id, Event<?> x,
            Worker worker) {

        return Observable.create(new SyncOnSubscribe<Signals, EntityStateMachine<?>>() {

            @Override
            protected Signals generateState() {
                Signals signals = new Signals();
                signals.signalsToSelf.offerFirst(x);
                return signals;
            }

            @Override
            protected Signals next(Signals signals,
                    Observer<? super EntityStateMachine<?>> observer) {
                EntityStateMachine<?> m = getStateMachine(cls, id);
                Event<?> event = signals.signalsToSelf.pollLast();
                if (event != null) {
                    m = m.signal(event);
                    // stateMachines.put(id, m);
                    observer.onNext(m);
                    List<Event<?>> list = m.signalsToSelf();
                    for (int i = list.size() - 1; i >= 0; i--) {
                        signals.signalsToSelf.offerLast(list.get(i));
                    }
                    for (Signal<?, ?> signal : m.signalsToOther()) {
                        signals.signalsToOther.offerFirst(signal);
                    }
                } else {
                    Signal<?, ?> signal;
                    while ((signal = signals.signalsToOther.pollLast()) != null) {
                        Signal<?, ?> s = signal;
                        if (signal.isImmediate()) {
                            subject.onNext(signal);
                        } else {
                            long delayMs = signal.time() - worker.now();
                            if (delayMs <= 0) {
                                subject.onNext(signal);
                            } else {
                                worker.schedule(() -> subject.onNext(s.now()), delayMs,
                                        TimeUnit.MILLISECONDS);
                            }
                        }
                    }
                    observer.onCompleted();
                }
                return signals;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> EntityStateMachine<T> getStateMachine(Class<T> cls, Id id) {
        EntityStateMachine<T> m = (EntityStateMachine<T>) stateMachines
                .get(new ClassId<Id>(cls, id));
        if (m == null) {
            m = (EntityStateMachine<T>) stateMachineFactory.call(cls, id);
        }
        return m;
    }

    public void signal(Signal<?, ?> signal) {
        subject.onNext(signal);
    }

    public <T, R> void signal(T object, Event<R> event) {
        subject.onNext(Signal.create(object, event));
    }

    @SuppressWarnings("unchecked")
    public <T> ObjectState<T> get(Class<T> cls, Id id) {
        return (EntityStateMachine<T>) stateMachines.get(new ClassId<Id>(cls, id));
    }

    public void onCompleted() {
        subject.onCompleted();
    }

}
