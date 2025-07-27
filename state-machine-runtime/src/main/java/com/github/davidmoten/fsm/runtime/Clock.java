package com.github.davidmoten.fsm.runtime;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Scheduler;

public interface Clock {

    long now();

    public static Clock from(Scheduler scheduler) {
        return new Clock() {
            @Override
            public long now() {
                return scheduler.now(TimeUnit.MILLISECONDS);
            }
        };

    }

}
