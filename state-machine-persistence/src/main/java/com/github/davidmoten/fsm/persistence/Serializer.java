package com.github.davidmoten.fsm.persistence;

public interface Serializer {

    byte[] serialize(Object t);

    Object deserialize(byte[] bytes);

}
