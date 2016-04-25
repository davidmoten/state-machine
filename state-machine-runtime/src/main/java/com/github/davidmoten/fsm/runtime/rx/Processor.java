package com.github.davidmoten.fsm.runtime.rx;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import rx.observables.SyncOnSubscribe;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public final class Processor<Id> {

	private final Func1<Object, Id> id;
	private final Func1<Id, EntityStateMachine<?>> stateMachineFactory;
	private final PublishSubject<Signal<?, ?>> subject;
	private final Map<Id, EntityStateMachine<?>> stateMachines = new ConcurrentHashMap<>();
	private final Scheduler scheduler;

	private Processor(Func1<Object, Id> id, Func1<Id, EntityStateMachine<?>> stateMachineFactory, Scheduler scheduler) {
		Preconditions.checkNotNull(id);
		Preconditions.checkNotNull(stateMachineFactory);
		Preconditions.checkNotNull(scheduler);
		this.id = id;
		this.stateMachineFactory = stateMachineFactory;
		this.scheduler = scheduler;
		this.subject = PublishSubject.create();
	}

	public static <Id> Processor<Id> create(Func1<Object, Id> id, Func1<Id, EntityStateMachine<?>> stateMachineFactory,
			Scheduler scheduler) {
		return new Processor<Id>(id, stateMachineFactory, scheduler);
	}

	public static <Id> Processor<Id> create(Func1<Object, Id> id,
			Func1<Id, EntityStateMachine<?>> stateMachineFactory) {
		return new Processor<Id>(id, stateMachineFactory, Schedulers.computation());
	}

	public Observable<EntityStateMachine<?>> observable() {
		return Observable.defer(() -> {
			Worker worker = scheduler.createWorker();
			return subject
					//
					.toSerialized()
					//
					.doOnNext(System.out::println)
					//
					.doOnUnsubscribe(() -> worker.unsubscribe())
					//
					.groupBy(signal -> id.call(signal.object()))
					//
					.flatMap(g -> g
							//
							.map(signal -> signal.event())
							//
							.flatMap(x -> process(g.getKey(), x, worker))
							//
							.doOnNext(m -> stateMachines.put(g.getKey(), m)));
		});
	}

	private static final class Signals {
		final Deque<Event<?>> signalsToSelf = new ArrayDeque<>();
		final Deque<Signal<?, ?>> signalsToOther = new ArrayDeque<>();
	}

	private Observable<EntityStateMachine<?>> process(Id id, Event<?> x, Worker worker) {

		return Observable.create(new SyncOnSubscribe<Signals, EntityStateMachine<?>>() {

			@Override
			protected Signals generateState() {
				Signals signals = new Signals();
				signals.signalsToSelf.offerFirst(x);
				return signals;
			}

			@Override
			protected Signals next(Signals signals, Observer<? super EntityStateMachine<?>> observer) {
				EntityStateMachine<?> m = getStateMachine(id);
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
						if (signal.delay() == 0) {
							subject.onNext(signal);
						} else {
							worker.schedule(() -> subject.onNext(s.now()), signal.delay(), signal.unit());
						}
					}
					observer.onCompleted();
				}
				return signals;
			}
		});
	}

	private EntityStateMachine<?> getStateMachine(Id id) {
		EntityStateMachine<?> m = stateMachines.get(id);
		if (m == null) {
			m = stateMachineFactory.call(id);
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
	public <T> ObjectState<T> get(Id id) {
		return (EntityStateMachine<T>) stateMachines.get(id);
	}

	public void onCompleted() {
		subject.onCompleted();
	}

}
