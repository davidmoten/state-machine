package com.github.davidmoten.fsm.runtime;

public interface Context {

    <T> void signal(T object, Event<?> event);

    void signalToSelf(Event<?> event);

}
