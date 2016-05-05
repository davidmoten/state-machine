package com.github.davidmoten.fsm.runtime;

public final class CancelTimedSignal<Id> implements Event<Object> {

    private final Class<?> fromClass;
    private final Id fromId;

    public CancelTimedSignal(Class<?> fromClass, Id fromId) {
        this.fromClass = fromClass;
        this.fromId = fromId;
    }

    public Class<?> fromClass() {
        return fromClass;
    }

    public Id fromId() {
        return fromId;
    }

}
