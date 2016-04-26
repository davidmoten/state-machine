package com.github.davidmoten.fsm.runtime.rx;

import java.util.HashMap;
import java.util.Map;

import com.github.davidmoten.util.Preconditions;

import rx.functions.Func1;

public class IdMapper<Id> implements Func1<Object, Id> {

    private final Map<Class<?>, Func1<?, ? extends Id>> map;

    private IdMapper(Map<Class<?>, Func1<?, ? extends Id>> map) {
        // make a defensive copy
        this.map = new HashMap<>(map);
    }

    public static <T, Id> Builder<Id> add(Class<T> cls, Func1<? super T, ? extends Id> idMapper) {
        return new Builder<Id>().add(cls, idMapper);
    }

    public static final class Builder<Id> {
        private final Map<Class<?>, Func1<?, ? extends Id>> map = new HashMap<>();

        public <T> Builder<Id> add(Class<T> cls, Func1<? super T, ? extends Id> idMapper) {
            map.put(cls, idMapper);
            return this;
        }

        public IdMapper<Id> build() {
            return new IdMapper<Id>(map);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Id call(Object o) {
        Preconditions.checkNotNull(o);
        Class<?> cls = o.getClass();
        Func1<?, ? extends Id> f = map.get(cls);
        if (f != null) {
            return ((Func1<Object, ? extends Id>) f).call(o);
        } else {
            throw new RuntimeException("identifier mapper not defined for class " + cls);
        }

    }

}
