package com.github.davidmoten.fsm.runtime;

import java.util.concurrent.TimeUnit;

public interface SignallerAsync<Id> {

    <R> void signal(Class<R> cls, Id id, Event<? super R> event);

    <R> void signal(Class<R> cls, Id id, Event<? super R> event, long delay, TimeUnit unit);

    long now();
}
