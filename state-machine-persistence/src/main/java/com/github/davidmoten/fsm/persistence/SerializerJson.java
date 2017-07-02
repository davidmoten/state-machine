package com.github.davidmoten.fsm.persistence;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class SerializerJson implements Serializer {

    private final ObjectMapper m = new ObjectMapper() //
            .setVisibility(PropertyAccessor.FIELD, Visibility.PUBLIC_ONLY)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

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
