package com.github.davidmoten.bean.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a field/parameter/variable/return type is never null.
 */
@Documented
@Target(value = {FIELD, METHOD, PARAMETER, LOCAL_VARIABLE})
@Retention(value = RetentionPolicy.RUNTIME)
public @interface NonNull { }

