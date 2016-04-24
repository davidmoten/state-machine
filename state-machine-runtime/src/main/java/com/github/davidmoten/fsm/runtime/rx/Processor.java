package com.github.davidmoten.fsm.runtime.rx;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.davidmoten.fsm.runtime.EntityStateMachine;
import com.github.davidmoten.fsm.runtime.Event;
import com.github.davidmoten.fsm.runtime.ObjectState;
import com.github.davidmoten.fsm.runtime.Signal;
import com.github.davidmoten.rx.Transformers;
import com.github.davidmoten.util.Preconditions;

import rx.Observable;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

public final class Processor<Id> {

	private final Func1<Object, Id> id;
	private final Func1<Id, EntityStateMachine<?>> stateMachineFactory;
	private final PublishSubject<Signal<?, ?>> subject;
	private final Map<Id, EntityStateMachine<?>> stateMachines = new ConcurrentHashMap<>();

	private Processor(Func1<Object, Id> id, Func1<Id, EntityStateMachine<?>> stateMachineFactory) {
		Preconditions.checkNotNull(id);
		Preconditions.checkNotNull(stateMachineFactory);
		this.id = id;
		this.stateMachineFactory = stateMachineFactory;
		this.subject = PublishSubject.create();
	}

	public static <Id> Processor<Id> create(Func1<Object, Id> id,
			Func1<Id, EntityStateMachine<?>> stateMachineFactory) {
		return new Processor<Id>(id, stateMachineFactory);
	}

	public Observable<EntityStateMachine<?>> asObservable() {
		return Observable.defer(() -> {
			Worker worker = Schedulers.computation().createWorker();
			return subject
					//
					.toSerialized()
					//
					.doOnUnsubscribe(() -> worker.unsubscribe())
					//
					.groupBy(id)
					//
					.flatMap(g -> g
							//
							.map(signal -> signal.event())
							//
							.flatMap(x -> process(g.getKey(), x, worker)));
		});
	}

	private Observable<EntityStateMachine<?>> process(Id id, Event<?> x, Worker worker) {

		Func0<Deque<Event<?>>> initialStateFactory = () -> new ArrayDeque<>();
		Func3<Deque<Event<?>>, Event<?>, Subscriber<EntityStateMachine<?>>, Deque<Event<?>>> transition = (q, ev,
				subscriber) -> {
			EntityStateMachine<?> m = stateMachines.get(id);
			if (m == null) {
				m = stateMachineFactory.call(id);
			}
			q.offerFirst(ev);
			List<Signal<?, ?>> signalsToOther = new ArrayList<>();
			Event<?> event;
			while ((event = q.pollLast()) != null) {
				m = m.event(event);
				subscriber.onNext(m);
				List<Event<?>> signalsToSelf = m.signalsToSelf();
				for (int i = signalsToSelf.size() - 1; i >= 0; i--) {
					q.offerLast(signalsToSelf.get(i));
				}
				signalsToOther.addAll(m.signalsToOther());
			}
			for (Signal<?, ?> signal : signalsToOther) {
				if (signal.delay() == 0) {
					subject.onNext(signal);
				} else {
					worker.schedule(() -> subject.onNext(signal.now()), signal.delay(), signal.unit());
				}
			}
			return q;
		};
		Func2<Deque<Event<?>>, Subscriber<EntityStateMachine<?>>, Boolean> completion = (q, sub) -> true;
		return Observable.just(x).compose(Transformers.stateMachine(initialStateFactory, transition, completion));
	}

	public void signal(Signal<?, ?> signal) {
		subject.onNext(signal);
	}

	@SuppressWarnings("unchecked")
	public <T> ObjectState<T> get(Id id) {
		return (EntityStateMachine<T>) stateMachines.get(id);
	}

}
