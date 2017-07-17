package com.github.davidmoten.bean;

import java.util.Date;

import com.github.davidmoten.bean.annotation.GenerateImmutable;
import com.github.davidmoten.bean.annotation.NonNull;

@GenerateImmutable
public final class Example {
    @NonNull
    String id;
    int number;
    Date[] values;
}