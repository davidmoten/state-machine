package com.github.davidmoten.fsm.runtime.rx;

import java.io.Closeable;
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
import com.github.davidmoten.rx.Schedulers;
import com.github.davidmoten.rx.Transformers;

import rx.Observable;
import rx.Scheduler.Worker;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.functions.Func3;
import rx.subjects.PublishSubject;

public final class Processor<Id> implements Closeable {

	private final PublishSubject<Signal<?, ?>> subject;
	private final Func1<Object, Id> id;
	private final Func1<Id, EntityStateMachine<?>> stateMachineCreator;
	private final Map<Id, EntityStateMachine<?>> stateMachines = new ConcurrentHashMap<>();
	private final Worker worker;

	public Processor(Func1<Object, Id> id, Func1<Id, EntityStateMachine<?>> stateMachineCreator) {
		this.subject = PublishSubject.create();
		this.id = id;
		this.stateMachineCreator = stateMachineCreator;
		this.worker = Schedulers.computation().createWorker();
	}

	public Observable<EntityStateMachine<?>> asObservable() {
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
						.flatMap(x -> process(g.getKey(), x)));
	}

	private Observable<EntityStateMachine<?>> process(Id id, Event<?> x) {

		Func0<Deque<Event<?>>> initialStateFactory = () -> new ArrayDeque<>();
		Func3<Deque<Event<?>>, Event<?>, Subscriber<EntityStateMachine<?>>, Deque<Event<?>>> transition = (q, ev,
				subscriber) -> {
			EntityStateMachine<?> m = stateMachines.get(id);
			if (m == null) {
				m = stateMachineCreator.call(id);
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

	@Override
	public void close() {
		worker.unsubscribe();
	}

}
