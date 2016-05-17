package com.github.davidmoten.fsm.runtime;

public interface SignallerWithAsync<T, Id> {

    Signaller<T, Id> sync();

    AsyncSignaller<Id> async();

}
