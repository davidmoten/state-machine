package com.github.davidmoten.fsm.runtime.rx;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.fsm.runtime.CancelTimedSignal;
import com.github.davidmoten.fsm.runtime.EntityStateMachine;
import com.github.davidmoten.fsm.runtime.Event;
import com.github.davidmoten.fsm.runtime.ObjectState;
import com.github.davidmoten.fsm.runtime.Signal;
import com.github.davidmoten.guavamini.Preconditions;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscription;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.observables.SyncOnSubscribe;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public final class Processor<Id> {

    private final Func1<Object, Id> idMapper;
    private final Func2<Class<?>, Id, EntityStateMachine<?>> stateMachineFactory;
    private final PublishSubject<Signal<?, ?>> subject;
    private final Scheduler signalScheduler;
    private final Scheduler processingScheduler;
    private final Map<ClassId<?>, EntityStateMachine<?>> stateMachines = new ConcurrentHashMap<>();
    private final Map<IdPair<Id>, Subscription> subscriptions = new ConcurrentHashMap<>();

    private Processor(Func1<Object, Id> idMapper,
            Func2<Class<?>, Id, EntityStateMachine<?>> stateMachineFactory,
            Scheduler processingScheduler, Scheduler signalScheduler) {
        Preconditions.checkNotNull(idMapper);
        Preconditions.checkNotNull(stateMachineFactory);
        Preconditions.checkNotNull(signalScheduler);
        this.idMapper = idMapper;
        this.stateMachineFactory = stateMachineFactory;
        this.signalScheduler = signalScheduler;
        this.processingScheduler = processingScheduler;
        this.subject = PublishSubject.create();
    }

    public static <Id> Builder<Id> idMapper(Func1<Object, Id> idMapper) {
        return new Builder<Id>().idMapper(idMapper);
    }

    public static <Id> Builder<Id> stateMachineFactory(
            Func2<Class<?>, Id, EntityStateMachine<?>> stateMachineFactory) {
        return new Builder<Id>().stateMachineFactory(stateMachineFactory);
    }

    public static <Id> Builder<Id> signalScheduler(Scheduler signalScheduler) {
        return new Builder<Id>().signalScheduler(signalScheduler);
    }

    public static <Id> Builder<Id> processingScheduler(Scheduler processingScheduler) {
        return new Builder<Id>().processingScheduler(processingScheduler);
    }

    public static class Builder<Id> {

        private Func1<Object, Id> idMapper;
        private Func2<Class<?>, Id, EntityStateMachine<?>> stateMachineFactory;
        private Scheduler signalScheduler = Schedulers.computation();
        private Scheduler processingScheduler = Schedulers.immediate();

        private Builder() {
        }

        public Builder<Id> idMapper(Func1<Object, Id> idMapper) {
            this.idMapper = idMapper;
            return this;
        }

        public Builder<Id> stateMachineFactory(
                Func2<Class<?>, Id, EntityStateMachine<?>> stateMachineFactory) {
            this.stateMachineFactory = stateMachineFactory;
            return this;
        }

        public Builder<Id> signalScheduler(Scheduler signalScheduler) {
            this.signalScheduler = signalScheduler;
            return this;
        }

        public Builder<Id> processingScheduler(Scheduler processingScheduler) {
            this.processingScheduler = processingScheduler;
            return this;
        }

        public Processor<Id> build() {
            return new Processor<Id>(idMapper, stateMachineFactory, processingScheduler,
                    signalScheduler);
        }

    }

    public Observable<EntityStateMachine<?>> observable() {
        return Observable.defer(() -> {
            Worker worker = signalScheduler.createWorker();
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
                        } else if (signal.event() instanceof CancelTimedSignal) {
                            Object from = ((CancelTimedSignal) signal.event()).from();
                            Object to = signal.object();
                            Subscription sub = subscriptions
                                    .remove(new IdPair<Id>(idMapper.call(from), idMapper.call(to)));
                            if (sub != null) {
                                sub.unsubscribe();
                            }
                        } else {
                            long delayMs = signal.time() - worker.now();
                            if (delayMs <= 0) {
                                subject.onNext(signal);
                            } else {
                                // record pairwise signal so we can cancel it if
                                // desired
                                IdPair<Id> idPair = new IdPair<Id>(id,
                                        idMapper.call(signal.object()));
                                long t1 = signalScheduler.now();
                                Subscription subscription = worker.schedule(() -> {
                                    subject.onNext(s.now());
                                } , delayMs, TimeUnit.MILLISECONDS);
                                long t2 = signalScheduler.now();
                                worker.schedule(() -> {
                                    subscriptions.remove(idPair);
                                } , delayMs - (t2 - t1), TimeUnit.MILLISECONDS);
                                Subscription previous = subscriptions.put(idPair, subscription);
                                if (previous != null) {
                                    previous.unsubscribe();
                                }
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

    public void cancelSignal(Object from, Object to) {
        Subscription subscription = subscriptions
                .remove(new IdPair<Id>(idMapper.call(from), idMapper.call(to)));
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }

}
