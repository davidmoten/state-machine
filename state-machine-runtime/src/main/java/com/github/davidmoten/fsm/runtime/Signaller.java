package com.github.davidmoten.fsm.runtime;

import java.util.concurrent.TimeUnit;

public interface Signaller<T> {

    void signalToSelf(Event<? super T> event);

    void signalToSelf(Event<? super T> event, long delay, TimeUnit unit);

    <R> void signal(Class<R> cls, Object id, Event<? super R> event);

    <R> void signal(Class<R> cls, Object id, Event<? super R> event, long delay, TimeUnit unit);

    void cancelSignal(Class<?> fromClass, Object fromId, Class<?> toClass, Object toId);

    long now();

}
