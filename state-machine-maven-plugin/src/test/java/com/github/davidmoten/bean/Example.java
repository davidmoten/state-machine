package com.github.davidmoten.bean;

import java.util.Date;

import com.github.davidmoten.bean.annotation.ImmutableBean;

@ImmutableBean
public final class Example {
    String id;
    int number;
    Date[] values;
}