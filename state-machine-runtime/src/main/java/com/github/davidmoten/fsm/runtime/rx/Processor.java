package com.github.davidmoten.fsm.runtime.rx;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.github.davidmoten.fsm.runtime.CancelTimedSignal;
import com.github.davidmoten.fsm.runtime.Clock;
import com.github.davidmoten.fsm.runtime.EntityBehaviour;
import com.github.davidmoten.fsm.runtime.EntityState;
import com.github.davidmoten.fsm.runtime.EntityStateMachine;
import com.github.davidmoten.fsm.runtime.Event;
import com.github.davidmoten.fsm.runtime.ObjectState;
import com.github.davidmoten.fsm.runtime.Search;
import com.github.davidmoten.fsm.runtime.Signal;
import com.github.davidmoten.guavamini.Preconditions;
import com.github.davidmoten.rx.Actions;
import com.github.davidmoten.rx.Functions;
import com.github.davidmoten.rx.Transformers;

import rx.Observable;
import rx.Observable.Transformer;
import rx.Observer;
import rx.Scheduler;
import rx.Scheduler.Worker;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Action3;
import rx.functions.Func1;
import rx.observables.GroupedObservable;
import rx.observables.SyncOnSubscribe;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public final class Processor<Id> {

    private final Func1<Class<?>, EntityBehaviour<?, Id>> behaviourFactory;
    private final PublishSubject<Signal<?, Id>> subject;
    private final Scheduler signalScheduler;
    private final Scheduler processingScheduler;
    private final Map<ClassId<?, Id>, EntityStateMachine<?, Id>> stateMachines = new ConcurrentHashMap<>();
    private final Map<ClassIdPair<Id>, Subscription> subscriptions = new ConcurrentHashMap<>();
    private final Observable<Signal<?, Id>> signals;
    private final Func1<GroupedObservable<ClassId<?, Id>, EntityStateMachine<?, Id>>, Observable<EntityStateMachine<?, Id>>> entityTransform;
    private final Transformer<Signal<?, Id>, Signal<?, Id>> preGroupBy;
    private final Func1<Action1<ClassId<?, Id>>, Map<ClassId<?, Id>, Object>> mapFactory; // nullable
    private final Clock signallerClock;
    private final Action1<? super EntityStateMachine<?, Id>> postTransitionAction;
    private final Action3<? super EntityStateMachine<?, Id>, ? super Optional<Event<?>>, ? super EntityState<?>> preTransitionAction;

    private final Search<Id> search = new Search<Id>() {
        @Override
        public <T> Optional<T> search(Class<T> cls, Id id) {
            return getStateMachine(cls, id).get();
        }
    };

    private Processor(Func1<Class<?>, EntityBehaviour<?, Id>> behaviourFactory,
            Scheduler processingScheduler, Scheduler signalScheduler,
            Observable<Signal<?, Id>> signals,
            Func1<GroupedObservable<ClassId<?, Id>, EntityStateMachine<?, Id>>, Observable<EntityStateMachine<?, Id>>> entityTransform,
            Transformer<Signal<?, Id>, Signal<?, Id>> preGroupBy,
            Func1<Action1<ClassId<?, Id>>, Map<ClassId<?, Id>, Object>> mapFactory,
            Action3<? super EntityStateMachine<?, Id>, ? super Optional<Event<?>>, ? super EntityState<?>> preTransitionAction,
            Action1<? super EntityStateMachine<?, Id>> postTransitionAction) {
        Preconditions.checkNotNull(behaviourFactory);
        Preconditions.checkNotNull(signalScheduler);
        Preconditions.checkNotNull(signals);
        Preconditions.checkNotNull(entityTransform);
        Preconditions.checkNotNull(preGroupBy);
        Preconditions.checkNotNull(preTransitionAction);
        Preconditions.checkNotNull(postTransitionAction);
        // mapFactory is nullable
        this.behaviourFactory = behaviourFactory;
        this.signalScheduler = signalScheduler;
        this.processingScheduler = processingScheduler;
        this.subject = PublishSubject.create();
        this.signals = signals;
        this.entityTransform = entityTransform;
        this.preGroupBy = preGroupBy;
        this.mapFactory = mapFactory;
        this.signallerClock = Clock.from(signalScheduler);
        this.preTransitionAction = preTransitionAction;
        this.postTransitionAction = postTransitionAction;
    }

    public static <Id> Builder<Id> behaviourFactory(
            Func1<Class<?>, EntityBehaviour<?, Id>> behaviourFactory) {
        return new Builder<Id>().behaviourFactory(behaviourFactory);
    }

    public static <T, Id> Builder<Id> behaviour(Class<T> cls, EntityBehaviour<T, Id> behaviour) {
        return new Builder<Id>().behaviour(cls, behaviour);
    }

    public static <Id> Builder<Id> signalScheduler(Scheduler signalScheduler) {
        return new Builder<Id>().signalScheduler(signalScheduler);
    }

    public static <Id> Builder<Id> processingScheduler(Scheduler processingScheduler) {
        return new Builder<Id>().processingScheduler(processingScheduler);
    }

    public static class Builder<Id> {

        private Func1<Class<?>, EntityBehaviour<?, Id>> behaviourFactory;
        private Scheduler signalScheduler = Schedulers.computation();
        private Scheduler processingScheduler = Schedulers.trampoline();
        private Observable<Signal<?, Id>> signals = Observable.empty();
        private Func1<GroupedObservable<ClassId<?, Id>, EntityStateMachine<?, Id>>, Observable<EntityStateMachine<?, Id>>> entityTransform = g -> g;
        private Transformer<Signal<?, Id>, Signal<?, Id>> preGroupBy = x -> x;
        private Func1<Action1<ClassId<?, Id>>, Map<ClassId<?, Id>, Object>> mapFactory; // nullable
        private Action3<? super EntityStateMachine<?, Id>, ? super Optional<Event<?>>, ? super EntityState<?>> preTransitionAction = Actions
                .doNothing3();
        private Action1<? super EntityStateMachine<?, Id>> postTransitionAction = Actions
                .doNothing1();
        private final Map<Class<?>, EntityBehaviour<?, Id>> behaviours = new HashMap<>();

        private Builder() {
        }

        public <T> Builder<Id> behaviour(Class<T> cls, EntityBehaviour<T, Id> behaviour) {
            behaviours.put(cls, behaviour);
            return this;
        }

        public Builder<Id> behaviourFactory(
                Func1<Class<?>, EntityBehaviour<?, Id>> behaviourFactory) {
            this.behaviourFactory = behaviourFactory;
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

        public Builder<Id> signals(Observable<Signal<?, Id>> signals) {
            this.signals = signals;
            return this;
        }

        public Builder<Id> entityTransform(
                Func1<GroupedObservable<ClassId<?, Id>, EntityStateMachine<?, Id>>, Observable<EntityStateMachine<?, Id>>> entityTransform) {
            this.entityTransform = entityTransform;
            return this;
        }

        public Builder<Id> preGroupBy(Transformer<Signal<?, Id>, Signal<?, Id>> preGroupBy) {
            this.preGroupBy = preGroupBy;
            return this;
        }

        public Builder<Id> mapFactory(
                Func1<Action1<ClassId<?, Id>>, Map<ClassId<?, Id>, Object>> mapFactory) {
            this.mapFactory = mapFactory;
            return this;
        }

        public Builder<Id> preTransition(
                Action3<? super EntityStateMachine<?, Id>, ? super Optional<Event<?>>, ? super EntityState<?>> action) {
            this.preTransitionAction = action;
            return this;
        }

        public Builder<Id> postTransition(Action1<? super EntityStateMachine<?, Id>> action) {
            this.postTransitionAction = action;
            return this;
        }

        public Processor<Id> build() {
            Preconditions.checkArgument(behaviourFactory != null || !behaviours.isEmpty(),
                    "one of behaviourFactory or multiple calls to behaviour must be made (behaviour must be specified)");
            Preconditions.checkArgument(behaviourFactory == null || behaviours.isEmpty(),
                    "cannot specify both behaviourFactory and behaviour");
            if (!behaviours.isEmpty()) {
                behaviourFactory = cls -> behaviours.get(cls);
            }
            return new Processor<Id>(behaviourFactory, processingScheduler, signalScheduler,
                    signals, entityTransform, preGroupBy, mapFactory, preTransitionAction,
                    postTransitionAction);
        }

    }

    public Observable<EntityStateMachine<?, Id>> observable() {
        return Observable.defer(() -> {
            Worker worker = signalScheduler.createWorker();
            @SuppressWarnings({ "unchecked", "rawtypes" })
            Observable<GroupedObservable<ClassId<?, Id>, Signal<?, Id>>> o1 = subject //
                    .toSerialized() //
                    .onBackpressureBuffer() //
                    .mergeWith(signals) //
                    .doOnUnsubscribe(() -> worker.unsubscribe()) //
                    .compose(preGroupBy) //
                    .compose(Transformers.groupByEvicting(
                            signal -> new ClassId(signal.cls(), signal.id()), x -> x, mapFactory));

            return o1.flatMap(g -> {
                Observable<EntityStateMachine<?, Id>> obs = g //
                        .flatMap(processSignalsToSelfAndSendSignalsToOthers(worker, g.getKey())) //
                        .doOnNext(m -> stateMachines.put(g.getKey(), m)) //
                        .subscribeOn(processingScheduler); //
                return entityTransform.call(GroupedObservable.from(g.getKey(), obs));
            });
        });
    }

    private Func1<? super Signal<?, Id>, Observable<EntityStateMachine<?, Id>>> processSignalsToSelfAndSendSignalsToOthers(
            Worker worker, ClassId<?, Id> classId) {
        return signal -> process(classId, signal.event(), worker).toList()
                .flatMapIterable(Functions.identity());
    }

    private static final class Signals<Id> {
        final Deque<Event<?>> signalsToSelf = new ArrayDeque<>();
        final Deque<Signal<?, Id>> signalsToOther = new ArrayDeque<>();
    }

    private Observable<EntityStateMachine<?, Id>> process(ClassId<?, Id> cid, Event<?> ev,
            Worker worker) {

        return Observable.create(new SyncOnSubscribe<Signals<Id>, EntityStateMachine<?, Id>>() {

            @SuppressWarnings("unchecked")
            EntityStateMachine<Object, Id> machine = (EntityStateMachine<Object, Id>) getStateMachine(
                    cid.cls(), cid.id());

            @Override
            protected Signals<Id> generateState() {
                Signals<Id> signals = new Signals<>();
                signals.signalsToSelf.offerFirst(ev);
                return signals;
            }

            @Override
            protected Signals<Id> next(Signals<Id> signals,
                    Observer<? super EntityStateMachine<?, Id>> observer) {
                @SuppressWarnings("unchecked")
                Event<Object> event = (Event<Object>) signals.signalsToSelf.pollLast();
                if (event != null) {
                    applySignalToSelf(signals, observer, event);
                } else {
                    applySignalsToOthers(cid, worker, signals);
                    observer.onCompleted();
                }
                return signals;
            }

            @SuppressWarnings("unchecked")
            private <T> void applySignalToSelf(Signals<Id> signals,
                    Observer<? super EntityStateMachine<?, Id>> observer, Event<T> event) {
                machine = machine.signal((Event<Object>) event);
                postTransitionAction.call(machine);
                // downstream synchronously updates the stateMachines
                observer.onNext(machine);
                List<Event<? super T>> list = (List<Event<? super T>>) (List<?>) machine
                        .signalsToSelf();
                for (int i = list.size() - 1; i >= 0; i--) {
                    signals.signalsToSelf.offerLast(list.get(i));
                }
                for (Signal<?, ?> signal : machine.signalsToOther()) {
                    signals.signalsToOther.offerFirst((Signal<?, Id>) signal);
                }
            }

            private void applySignalsToOthers(ClassId<?, Id> cid, Worker worker,
                    Signals<Id> signals) {
                Signal<?, Id> signal;
                while ((signal = signals.signalsToOther.pollLast()) != null) {
                    Signal<?, Id> s = signal;
                    if (signal.isImmediate()) {
                        subject.onNext(signal);
                    } else if (signal.event() instanceof CancelTimedSignal) {
                        cancel(signal);
                    } else {
                        long delayMs = signal.time().get() - worker.now();
                        if (delayMs <= 0) {
                            subject.onNext(signal);
                        } else {
                            scheduleSignal(cid, worker, signal, s, delayMs);
                        }
                    }
                }
            }

            private void cancel(Signal<?, Id> signal) {
                @SuppressWarnings("unchecked")
                CancelTimedSignal<Id> s = ((CancelTimedSignal<Id>) signal.event());
                @SuppressWarnings({ "unchecked", "rawtypes" })
                Subscription sub = subscriptions
                        .remove(new ClassIdPair<Id>(new ClassId(s.fromClass(), s.fromId()),
                                new ClassId(signal.cls(), signal.id())));
                if (sub != null) {
                    sub.unsubscribe();
                }
            }

            private void scheduleSignal(ClassId<?, Id> from, Worker worker, Signal<?, Id> signal,
                    Signal<?, Id> s, long delayMs) {
                // record pairwise signal so we can cancel it if
                // desired
                @SuppressWarnings({ "unchecked", "rawtypes" })
                ClassIdPair<Id> idPair = new ClassIdPair<Id>(from,
                        new ClassId(signal.cls(), signal.id()));
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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private <T> EntityStateMachine<T, Id> getStateMachine(Class<T> cls, Id id) {
        return (EntityStateMachine<T, Id>) stateMachines //
                .computeIfAbsent(new ClassId<T, Id>(cls, id),
                        clsId -> (EntityStateMachine<T, Id>) behaviourFactory.call(cls) //
                                .create(id) //
                                .withSearch(search) //
                                .withClock(signallerClock) //
                                .withPreTransition((Action3) preTransitionAction));
    }

    public <T> Optional<T> getObject(Class<T> cls, Id id) {
        return getStateMachine(cls, id).get();
    }

    public void signal(Signal<?, Id> signal) {
        subject.onNext(signal);
    }

    public <T> void signal(Class<T> cls, Id id, Event<? super T> event) {
        subject.onNext(Signal.create(cls, id, event));
    }

    public <T> void signal(ClassId<T, Id> cid, Event<? super T> event) {
        signal(cid.cls(), cid.id(), event);
    }

    @SuppressWarnings("unchecked")
    public <T> ObjectState<T> get(Class<T> cls, Id id) {
        return (EntityStateMachine<T, Id>) stateMachines.get(new ClassId<T, Id>(cls, id));
    }

    public void onCompleted() {
        subject.onCompleted();
    }

    public void cancelSignal(Class<?> fromClass, Id fromId, Class<?> toClass, Id toId) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Subscription subscription = subscriptions.remove(
                new ClassIdPair<Id>(new ClassId(fromClass, fromId), new ClassId(toClass, toId)));
        if (subscription != null) {
            subscription.unsubscribe();
        }
    }

    public void cancelSignalToSelf(Class<?> cls, Id id) {
        cancelSignal(cls, id, cls, id);
    }

    public void cancelSignalToSelf(ClassId<?, Id> cid) {
        cancelSignalToSelf(cid.cls(), cid.id());
    }

}
