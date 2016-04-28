package com.github.davidmoten.fsm.runtime.rx;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import rx.functions.Func2;
import rx.observables.SyncOnSubscribe;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public final class Processor<Id> {

    private final Func2<Class<?>, Id, EntityStateMachine<?>> stateMachineFactory;
    private final PublishSubject<Signal<?, Id, ?>> subject;
    private final Scheduler signalScheduler;
    private final Scheduler processingScheduler;
    private final Map<ClassId<?>, EntityStateMachine<?>> stateMachines = new ConcurrentHashMap<>();
    private final Map<ClassIdPair<Id>, Subscription> subscriptions = new ConcurrentHashMap<>();

    private Processor(Func2<Class<?>, Id, EntityStateMachine<?>> stateMachineFactory,
            Scheduler processingScheduler, Scheduler signalScheduler) {
        Preconditions.checkNotNull(stateMachineFactory);
        Preconditions.checkNotNull(signalScheduler);
        this.stateMachineFactory = stateMachineFactory;
        this.signalScheduler = signalScheduler;
        this.processingScheduler = processingScheduler;
        this.subject = PublishSubject.create();
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

        private Func2<Class<?>, Id, EntityStateMachine<?>> stateMachineFactory;
        private Scheduler signalScheduler = Schedulers.computation();
        private Scheduler processingScheduler = Schedulers.immediate();

        private Builder() {
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
            return new Processor<Id>(stateMachineFactory, processingScheduler, signalScheduler);
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
                    .groupBy(signal -> new ClassId<Id>(signal.cls(), signal.id()))
                    //
                    .flatMap(g -> g
                            //
                            .flatMap(x -> process(g.getKey(), x.event(), worker))
                            //
                            .doOnNext(m -> stateMachines.put(g.getKey(), m))
                            //
                            .subscribeOn(processingScheduler));
        });
    }

    private static final class Signals<Id> {
        final Deque<Event<?>> signalsToSelf = new ArrayDeque<>();
        final Deque<Signal<?, Id, ?>> signalsToOther = new ArrayDeque<>();
    }

    private Observable<EntityStateMachine<?>> process(ClassId<Id> cid, Event<?> x, Worker worker) {

        return Observable.create(new SyncOnSubscribe<Signals<Id>, EntityStateMachine<?>>() {

            @Override
            protected Signals<Id> generateState() {
                Signals<Id> signals = new Signals<>();
                signals.signalsToSelf.offerFirst(x);
                return signals;
            }

            @Override
            protected Signals<Id> next(Signals<Id> signals,
                    Observer<? super EntityStateMachine<?>> observer) {
                EntityStateMachine<?> m = getStateMachine(cid.cls(), cid.id());
                Event<?> event = signals.signalsToSelf.pollLast();
                if (event != null) {
                    applySignalToSelf(signals, observer, m, event);
                } else {
                    applySignalsToOthers(cid, worker, signals);
                    observer.onCompleted();
                }
                return signals;
            }

            @SuppressWarnings("unchecked")
            private void applySignalToSelf(Signals<Id> signals,
                    Observer<? super EntityStateMachine<?>> observer, EntityStateMachine<?> m,
                    Event<?> event) {
                m = m.signal(event);
                // stateMachines.put(id, m);
                observer.onNext(m);
                List<Event<?>> list = m.signalsToSelf();
                for (int i = list.size() - 1; i >= 0; i--) {
                    signals.signalsToSelf.offerLast(list.get(i));
                }
                for (Signal<?, ?, ?> signal : m.signalsToOther()) {
                    signals.signalsToOther.offerFirst((Signal<?, Id, ?>) signal);
                }
            }

            private void applySignalsToOthers(ClassId<Id> cid, Worker worker, Signals<Id> signals) {
                Signal<?, Id, ?> signal;
                while ((signal = signals.signalsToOther.pollLast()) != null) {
                    Signal<?, Id, ?> s = signal;
                    if (signal.isImmediate()) {
                        subject.onNext(signal);
                    } else if (signal.event() instanceof CancelTimedSignal) {
                        cancel(signal);
                    } else {
                        long delayMs = signal.time() - worker.now();
                        if (delayMs <= 0) {
                            subject.onNext(signal);
                        } else {
                            scheduleSignal(cid, worker, signal, s, delayMs);
                        }
                    }
                }
            }

            private void cancel(Signal<?, Id, ?> signal) {
                @SuppressWarnings("unchecked")
                CancelTimedSignal<Id> s = ((CancelTimedSignal<Id>) signal.event());
                Subscription sub = subscriptions
                        .remove(new ClassIdPair<Id>(new ClassId<Id>(s.fromClass(), s.fromId()),
                                new ClassId<Id>(signal.cls(), signal.id())));
                if (sub != null) {
                    sub.unsubscribe();
                }
            }

            private void scheduleSignal(ClassId<Id> from, Worker worker, Signal<?, Id, ?> signal,
                    Signal<?, Id, ?> s, long delayMs) {
                // record pairwise signal so we can cancel it if
                // desired
                ClassIdPair<Id> idPair = new ClassIdPair<Id>(from,
                        new ClassId<Id>(signal.cls(), signal.id()));
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

    public <T> Optional<T> getObject(Class<T> cls, Id id) {
        return getStateMachine(cls, id).get();
    }

    public void signal(Signal<?, Id, ?> signal) {
        subject.onNext(signal);
    }

    public <T, R> void signal(Class<T> cls, Id id, Event<R> event) {
        subject.onNext(Signal.create(cls, id, event));
    }

    public <T> void signal(ClassId<Id> cid, Event<T> event) {
        subject.onNext(Signal.create(cid.cls(), cid.id(), event));
    }

    @SuppressWarnings("unchecked")
    public <T> ObjectState<T> get(Class<T> cls, Id id) {
        return (EntityStateMachine<T>) stateMachines.get(new ClassId<Id>(cls, id));
    }

    public void onCompleted() {
        subject.onCompleted();
    }

    public void cancelSignal(Class<?> fromClass, Id fromId, Class<?> toClass, Id toId) {
        Subscription subscription = subscriptions.remove(new ClassIdPair<Id>(
                new ClassId<Id>(fromClass, fromId), new ClassId<Id>(toClass, toId)));
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }

    public void cancelSignalToSelf(Class<?> cls, Id id) {
        cancelSignal(cls, id, cls, id);
    }

}
