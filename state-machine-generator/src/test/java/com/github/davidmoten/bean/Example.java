package com.github.davidmoten.bean;

import java.io.Serializable;
import java.util.Date;

import com.github.davidmoten.bean.annotation.GenerateImmutable;
import com.github.davidmoten.bean.annotation.NonNull;

@SuppressWarnings("serial")
@GenerateImmutable
public final class Example implements Serializable{
    /**
     * identifier.
     */
    @NonNull
    String id;
    int number;
    Date[] values;
}