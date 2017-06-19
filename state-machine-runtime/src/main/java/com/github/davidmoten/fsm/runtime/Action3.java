package com.github.davidmoten.fsm.runtime;

/**
 * A three-argument action.
 * @param <T1> the first argument type
 * @param <T2> the second argument type
 * @param <T3> the third argument type
 */
public interface Action3<T1, T2, T3> {
    void call(T1 t1, T2 t2, T3 t3);
}
