package com.github.davidmoten.fsm.runtime.rx;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Subscriber;

import com.github.davidmoten.fsm.runtime.Action3;
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

import io.reactivex.BackpressureStrategy;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import io.reactivex.FlowableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.Scheduler.Worker;
import io.reactivex.disposables.Disposable;
import io.reactivex.flowables.GroupedFlowable;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.internal.functions.Functions;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;

public final class Processor<Id> {

    private final Function<Class<?>, EntityBehaviour<?, Id>> behaviourFactory;
    private final PublishSubject<Signal<?, Id>> subject;
    private final Scheduler signalScheduler;
    private final Scheduler processingScheduler;
    private final Map<ClassId<?, Id>, EntityStateMachine<?, Id>> stateMachines = new ConcurrentHashMap<>();
    private final Map<ClassIdPair<Id>, Disposable> subscriptions = new ConcurrentHashMap<>();
    private final Flowable<Signal<?, Id>> signals;
    private final Function<GroupedFlowable<ClassId<?, Id>, EntityStateMachine<?, Id>>, Flowable<EntityStateMachine<?, Id>>> entityTransform;
    private final FlowableTransformer<Signal<?, Id>, Signal<?, Id>> preGroupBy;
    private final Function<Consumer<ClassId<?, Id>>, Map<ClassId<?, Id>, Object>> mapFactory; // nullable
    private final Clock signallerClock;
    private final Action3<? super EntityStateMachine<?, Id>, ? super Event<?>, ? super EntityState<?>> preTransitionAction;
    private final Consumer<? super EntityStateMachine<?, Id>> postTransitionAction;

    private final Search<Id> search = new Search<Id>() {
        @Override
        public <T> Optional<T> search(Class<T> cls, Id id) {
            return getStateMachine(cls, id).get();
        }
    };

    private Processor(Function<Class<?>, EntityBehaviour<?, Id>> behaviourFactory,
            Scheduler processingScheduler, Scheduler signalScheduler,
            Flowable<Signal<?, Id>> signals,
            Function<GroupedFlowable<ClassId<?, Id>, EntityStateMachine<?, Id>>, Flowable<EntityStateMachine<?, Id>>> entityTransform,
            FlowableTransformer<Signal<?, Id>, Signal<?, Id>> preGroupBy,
            Function<Consumer<ClassId<?, Id>>, Map<ClassId<?, Id>, Object>> mapFactory,
            Action3<? super EntityStateMachine<?, Id>, ? super Event<?>, ? super EntityState<?>> preTransitionAction,
            Consumer<? super EntityStateMachine<?, Id>> postTransitionAction) {
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
            Function<Class<?>, EntityBehaviour<?, Id>> behaviourFactory) {
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

        private Function<Class<?>, EntityBehaviour<?, Id>> behaviourFactory;
        private Scheduler signalScheduler = Schedulers.computation();
        private Scheduler processingScheduler = Schedulers.trampoline();
        private Flowable<Signal<?, Id>> signals = Flowable.empty();
        private Function<GroupedFlowable<ClassId<?, Id>, EntityStateMachine<?, Id>>, Flowable<EntityStateMachine<?, Id>>> entityTransform = g -> g;
        private FlowableTransformer<Signal<?, Id>, Signal<?, Id>> preGroupBy = x -> x;
        private Function<Consumer<ClassId<?, Id>>, Map<ClassId<?, Id>, Object>> mapFactory; // nullable
        private Action3<? super EntityStateMachine<?, Id>, ? super Event<?>, ? super EntityState<?>> preTransitionAction = (
                x, y, z) -> {
        };
        private Consumer<? super EntityStateMachine<?, Id>> postTransitionAction = x -> {
        };
        private final Map<Class<?>, EntityBehaviour<?, Id>> behaviours = new HashMap<>();

        private Builder() {
        }

        public <T> Builder<Id> behaviour(Class<T> cls, EntityBehaviour<T, Id> behaviour) {
            behaviours.put(cls, behaviour);
            return this;
        }

        public Builder<Id> behaviourFactory(
                Function<Class<?>, EntityBehaviour<?, Id>> behaviourFactory) {
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

        public Builder<Id> signals(Flowable<Signal<?, Id>> signals) {
            this.signals = signals;
            return this;
        }

        public Builder<Id> entityTransform(
                Function<GroupedFlowable<ClassId<?, Id>, EntityStateMachine<?, Id>>, Flowable<EntityStateMachine<?, Id>>> entityTransform) {
            this.entityTransform = entityTransform;
            return this;
        }

        public Builder<Id> preGroupBy(
                FlowableTransformer<Signal<?, Id>, Signal<?, Id>> preGroupBy) {
            this.preGroupBy = preGroupBy;
            return this;
        }

        public Builder<Id> mapFactory(
                Function<Consumer<ClassId<?, Id>>, Map<ClassId<?, Id>, Object>> mapFactory) {
            this.mapFactory = mapFactory;
            return this;
        }

        public Builder<Id> preTransition(
                Action3<? super EntityStateMachine<?, Id>, ? super Event<?>, ? super EntityState<?>> action) {
            this.preTransitionAction = action;
            return this;
        }

        public Builder<Id> postTransition(Consumer<? super EntityStateMachine<?, Id>> action) {
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

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Flowable<EntityStateMachine<?, Id>> flowable() {
        return Flowable.defer(() -> {
            Worker worker = signalScheduler.createWorker();
            Flowable<Signal<?, Id>> o0 = subject //
                    .toSerialized() //
                    .toFlowable(BackpressureStrategy.BUFFER) //
                    .mergeWith(signals) //
                    .doOnCancel(() -> worker.dispose()) //
                    .compose(preGroupBy);
            Flowable<GroupedFlowable<ClassId<?, Id>, Signal<?, Id>>> o;
            if (mapFactory != null) {
                throw new UnsupportedOperationException(
                        "cannot use mapFactory in RxJava2, author will need to get API supplemented in RxJava2");
                // o1 = o0.groupBy(signal -> new ClassId(signal.cls(),
                // signal.id()), x -> x, mapFactory);
            } else {
                o = o0.groupBy(signal -> new ClassId(signal.cls(), signal.id()),
                        Functions.identity());
            }
            return o.flatMap(g -> {
                Flowable<EntityStateMachine<?, Id>> obs = g //
                        .flatMap(processSignalsToSelfAndSendSignalsToOthers(worker, g.getKey())) //
                        .doOnNext(m -> stateMachines.put(g.getKey(), m)) //
                        .subscribeOn(processingScheduler); //

                Flowable<EntityStateMachine<?, Id>> res = entityTransform
                        .apply(grouped(g.getKey(), obs));
                return res;
            });
        });
    }

    private static <K, T> GroupedFlowable<K, T> grouped(K key, final Flowable<T> o) {
        return new GroupedFlowable<K, T>(key) {
            @Override
            protected void subscribeActual(Subscriber<? super T> s) {
                o.subscribe(s);
            }
        };
    }

    private Function<? super Signal<?, Id>, Flowable<EntityStateMachine<?, Id>>> processSignalsToSelfAndSendSignalsToOthers(
            Worker worker, ClassId<?, Id> classId) {
        return signal -> process(classId, signal.event(), worker) //
                .toList() //
                .toFlowable().flatMapIterable(Functions.identity());
    }

    private static final class Signals<Id> {
        final Deque<Event<?>> signalsToSelf = new ArrayDeque<>();
        final Deque<Signal<?, Id>> signalsToOther = new ArrayDeque<>();
    }

    private Flowable<EntityStateMachine<?, Id>> process(ClassId<?, Id> classId, Event<?> event,
            Worker worker) {

        EntityStateMachine<?, Id> machine = getStateMachine(classId.cls(), classId.id());
        Generator generator = new Generator(classId, event, worker, machine);
        return Flowable.generate(generator, generator);
    }

    // note has access to surrounding classes' state because is not static
    private final class Generator implements Callable<Signals<Id>>,
            BiConsumer<Signals<Id>, Emitter<EntityStateMachine<?, Id>>> {

        private final Event<?> event;
        private final ClassId<?, Id> classId;
        private final Worker worker;

        // mutable
        EntityStateMachine<?, Id> machine;

        Generator(ClassId<?, Id> classId, Event<?> event, Worker worker,
                EntityStateMachine<?, Id> machine) {
            this.classId = classId;
            this.event = event;
            this.worker = worker;
            this.machine = machine;
        }

        @Override
        // generate state
        public Signals<Id> call() throws Exception {
            Signals<Id> signals = new Signals<>();
            signals.signalsToSelf.offerFirst(event);
            return signals;
        }

        @Override
        public void accept(Signals<Id> signals, Emitter<EntityStateMachine<?, Id>> observer)
                throws Exception {
            @SuppressWarnings("unchecked")
            Event<Object> event = (Event<Object>) signals.signalsToSelf.pollLast();
            if (event != null) {
                applySignalToSelf(signals, observer, event);
            } else {
                applySignalsToOthers(classId, worker, signals);
                observer.onComplete();
            }
        }

        @SuppressWarnings("unchecked")
        private <T> void applySignalToSelf(Signals<Id> signals,
                Emitter<? super EntityStateMachine<?, Id>> observer, Event<T> event)
                        throws Exception {
            machine = machine.signal((Event<Object>) event);
            postTransitionAction.accept(machine);
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

        private void applySignalsToOthers(ClassId<?, Id> cid, Worker worker, Signals<Id> signals) {
            Signal<?, Id> signal;
            while ((signal = signals.signalsToOther.pollLast()) != null) {
                Signal<?, Id> s = signal;
                if (signal.isImmediate()) {
                    subject.onNext(signal);
                } else if (signal.event() instanceof CancelTimedSignal) {
                    cancel(signal);
                } else {
                    long delayMs = signal.time().get() - worker.now(TimeUnit.MILLISECONDS);
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
            Disposable sub = subscriptions
                    .remove(new ClassIdPair<Id>(new ClassId(s.fromClass(), s.fromId()),
                            new ClassId(signal.cls(), signal.id())));
            if (sub != null) {
                sub.dispose();
            }
        }

        private void scheduleSignal(ClassId<?, Id> from, Worker worker, Signal<?, Id> signal,
                Signal<?, Id> s, long delayMs) {
            // record pairwise signal so we can cancel it if
            // desired
            @SuppressWarnings({ "unchecked", "rawtypes" })
            ClassIdPair<Id> idPair = new ClassIdPair<Id>(from,
                    new ClassId(signal.cls(), signal.id()));
            long t1 = signalScheduler.now(TimeUnit.MILLISECONDS);
            Disposable subscription = worker.schedule(() -> {
                subject.onNext(s.now());
            } , delayMs, TimeUnit.MILLISECONDS);
            long t2 = signalScheduler.now(TimeUnit.MILLISECONDS);
            worker.schedule(() -> {
                subscriptions.remove(idPair);
            } , delayMs - (t2 - t1), TimeUnit.MILLISECONDS);
            Disposable previous = subscriptions.put(idPair, subscription);
            if (previous != null) {
                previous.dispose();
            }
        }

    }

    @SuppressWarnings({ "unchecked" })
    private <T> EntityStateMachine<T, Id> getStateMachine(Class<T> cls, Id id) {
        return (EntityStateMachine<T, Id>) stateMachines //
                .computeIfAbsent(new ClassId<T, Id>(cls, id), clsId -> {
                    try {
                        return (EntityStateMachine<T, Id>) behaviourFactory.apply(cls) //
                                .create(id) //
                                .withSearch(search) //
                                .withClock(signallerClock) //
                                .withPreTransition(preTransitionAction);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public <T> Optional<T> getObject(Class<T> cls, Id id) {
        try {
            return getStateMachine(cls, id).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        subject.onComplete();
    }

    public void cancelSignal(Class<?> fromClass, Id fromId, Class<?> toClass, Id toId) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Disposable subscription = subscriptions.remove(
                new ClassIdPair<Id>(new ClassId(fromClass, fromId), new ClassId(toClass, toId)));
        if (subscription != null) {
            subscription.dispose();
        }
    }

    public void cancelSignalToSelf(Class<?> cls, Id id) {
        cancelSignal(cls, id, cls, id);
    }

    public void cancelSignalToSelf(ClassId<?, Id> cid) {
        cancelSignalToSelf(cid.cls(), cid.id());
    }

}
