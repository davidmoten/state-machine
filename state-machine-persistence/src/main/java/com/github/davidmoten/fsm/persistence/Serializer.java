package com.github.davidmoten.fsm.persistence;

public interface Serializer {

    byte[] serialize(Object t);

    <T> T deserialize(Class<T> cls, byte[] bytes);

    public static final Serializer JSON = new SerializerJson();

}
