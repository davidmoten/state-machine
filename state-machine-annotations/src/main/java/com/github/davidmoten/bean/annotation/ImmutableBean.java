package com.github.davidmoten.bean.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Documented
@Target(value = { ElementType.TYPE })
@Retention(value = RetentionPolicy.CLASS)
public @interface ImmutableBean {
}
