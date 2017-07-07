package com.github.davidmoten.fsm.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.github.davidmoten.fsm.persistence.Persistence.EntityAndState;
import com.github.davidmoten.fsm.persistence.Persistence.EntityWithId;
import com.github.davidmoten.fsm.persistence.exceptions.EntitiesNotSetException;

public interface Entities {

    <T> Optional<EntityAndState<T>> getWithState(Class<T> cls, String id);

    <T> Optional<T> get(Class<T> cls, String id);

    <T> List<EntityWithId<T>> get(Class<T> cls);

    <T> Set<EntityWithId<T>> get(Class<T> cls, String key, String value);

    <T> Set<EntityWithId<T>> getOr(Class<T> cls, Iterable<Property> properties);

    <T> Set<EntityWithId<T>> getAnd(Class<T> cls, Iterable<Property> properties);

    static final ThreadLocal<Entities> current = new ThreadLocal<Entities>();

    public static void set(Entities entities) {
        current.set(entities);
    }

    public static Entities get() {
        Entities e = current.get();
        if (e == null) {
            throw new EntitiesNotSetException();
        }
        return e;
    }

    public static void clear() {
        current.remove();
    }

}