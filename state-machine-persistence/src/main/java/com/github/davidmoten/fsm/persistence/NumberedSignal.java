package com.github.davidmoten.fsm.persistence;

import com.github.davidmoten.fsm.runtime.Signal;

public final class NumberedSignal<T, Id> {
    final Signal<T, Id> signal;
    final long number;

    public NumberedSignal(Signal<T, Id> signal, long number) {
        this.signal = signal;
        this.number = number;
    }
}
