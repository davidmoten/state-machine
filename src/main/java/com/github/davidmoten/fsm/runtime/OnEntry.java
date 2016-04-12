package com.github.davidmoten.fsm.runtime;

import com.github.davidmoten.fsm.model.Event;

public interface OnEntry<T, R extends Event<R>> {
    void onEntry(T object, R event);
}
