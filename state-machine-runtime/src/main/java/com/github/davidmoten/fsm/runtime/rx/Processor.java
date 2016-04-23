package com.github.davidmoten.fsm.runtime.rx;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.davidmoten.fsm.runtime.EntityStateMachine;
import com.github.davidmoten.fsm.runtime.ObjectState;
import com.github.davidmoten.fsm.runtime.Signal;

import rx.Observable;
import rx.functions.Func1;
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
				.flatMap(g -> g.flatMap(x -> Observable.from(getSignals(g.getKey(), x))));
	}

	private List<EntityStateMachine<?>> getSignals(Id id, Signal<?, ?> x) {
	}

	public void signal(Signal<?, ?> signal) {
		subject.onNext(signal);
	}

	@SuppressWarnings("unchecked")
	public <T> ObjectState<T> get(Id id) {
		return (EntityStateMachine<T>) stateMachines.get(id);
	}

}
