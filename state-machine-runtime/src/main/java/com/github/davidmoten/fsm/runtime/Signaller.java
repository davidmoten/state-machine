package com.github.davidmoten.fsm.runtime;

import java.util.concurrent.TimeUnit;

public interface Signaller {

    void signalToSelf(Event<?> event);

    void signalToSelf(Event<?> event, long delay, TimeUnit unit);

    void signal(Class<?> cls, Object id, Event<?> event);

    void signal(Class<?> cls, Object id, Event<?> event, long delay, TimeUnit unit);

    void cancelSignal(Class<?> fromClass, Object fromId, Class<?> toClass, Object toId);

    long now();

}
