package com.github.davidmoten.bean;

import java.util.Date;

import com.github.davidmoten.bean.annotation.ImmutableBean;
import com.github.davidmoten.bean.annotation.NonNull;

@ImmutableBean
public final class Example {
    @NonNull
    String id;
    int number;
    Date[] values;
}