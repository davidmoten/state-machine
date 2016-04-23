package com.github.davidmoten.fsm.runtime;

public interface Signaller {


    void signalToSelf(Event<?> event);

	<T> void signal(T object, Event<?> event);

}
