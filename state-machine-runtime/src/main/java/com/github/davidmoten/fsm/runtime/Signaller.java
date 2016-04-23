package com.github.davidmoten.fsm.runtime;

import java.util.concurrent.TimeUnit;

public interface Signaller {

    void signalToSelf(Event<?> event);
    
    void signalToSelf(Event<?> event, long duration, TimeUnit unit);

	<T> void signal(T object, Event<?> event);
	
	<T> void signal(T object, Event<?> event, long duration, TimeUnit unit);

}
