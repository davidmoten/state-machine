package com.github.davidmoten.fsm.persistence;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SerializerJson implements Serializer {

    private final ObjectMapper m = new ObjectMapper();

    @Override
    public byte[] serialize(Object t) {
        try {
            return m.writeValueAsBytes(t);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T deserialize(Class<T> cls, byte[] bytes) {
        try {
            return m.readValue(bytes, cls);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
