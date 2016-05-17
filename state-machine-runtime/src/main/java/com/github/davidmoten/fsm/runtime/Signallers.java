package com.github.davidmoten.fsm.runtime;

public interface Signallers<T, Id> {

    Signaller<T, Id> sync();

    SignallerAsync<Id> async();

}
