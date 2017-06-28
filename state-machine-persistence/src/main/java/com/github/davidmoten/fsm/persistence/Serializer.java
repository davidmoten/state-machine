package com.github.davidmoten.fsm.persistence;

public interface Serializer<T> {

    byte[] serialize(T t);

    T deserialize(byte[] bytes);

}
