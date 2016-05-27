package com.github.davidmoten.fsm.runtime.rx;

import java.util.HashMap;
import java.util.Map;

import com.github.davidmoten.fsm.runtime.EntityBehaviour;

import rx.functions.Func1;

public class StateMachineFactory<Id> implements Func1<Class<?>, EntityBehaviour<?, Id>> {

    private final Map<Class<?>, Func1<Class<?>, EntityBehaviour<?, Id>>> map;

    private StateMachineFactory(Map<Class<?>, Func1<Class<?>, EntityBehaviour<?, Id>>> map) {
        this.map = map;
    }

    public static <T> Builder2<T> cls(Class<T> cls) {
        return new Builder2<T>(cls, new Builder<Object>());
    }

    public static final class Builder2<T> {

        private final Class<T> cls;
        private final Builder<Object> builder;

        private Builder2(Class<T> cls, Builder<Object> builder) {
            this.cls = cls;
            this.builder = builder;
        }

        @SuppressWarnings("unchecked")
        public <Id> Builder<Id> hasFactory(
                Func1<Class<?>, ? extends EntityBehaviour<T, Id>> factory) {
            return ((Builder<Id>) builder).add(cls, factory);
        }
    }

    public static final class Builder<Id> {

        private final Map<Class<?>, Func1<Class<?>, EntityBehaviour<?, Id>>> map = new HashMap<>();

        private Builder() {
            // prevent instantiation publicly
        }

        @SuppressWarnings("unchecked")
        private <T> Builder<Id> add(Class<T> cls,
                Func1<Class<?>, ? extends EntityBehaviour<T, Id>> factory) {
            map.put(cls, (Func1<Class<?>, EntityBehaviour<?, Id>>) (Func1<? super Id, ?>) factory);
            return this;
        }

        @SuppressWarnings("unchecked")
        public <T> Builder2<T> cls(Class<T> cls) {
            return new Builder2<T>(cls, (Builder<Object>) this);
        }

        public StateMachineFactory<Id> build() {
            return new StateMachineFactory<Id>(map);
        }

    }

    @Override
    public EntityBehaviour<?, Id> call(Class<?> cls) {
        Func1<Class<?>, EntityBehaviour<?, Id>> f = map.get(cls);
        if (f != null) {
            return f.call(cls);
        } else {
            throw new RuntimeException("state machine factory not defined for " + cls);
        }
    }

}
