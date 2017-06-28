package com.github.davidmoten.fsm.persistence;

public final class SQLRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1990803598168507171L;

    public SQLRuntimeException(Throwable e) {
        super(e);
    }

}
