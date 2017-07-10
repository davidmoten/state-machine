package com.github.davidmoten.fsm.persistence;

public final class IntProperty {

    public final String name;
    public final int value;

    public IntProperty(String name, int value) {
        this.name = name;
        this.value = value;
    }
    
    public static IntProperty create(String name, int value) {
        return new IntProperty(name,value);
    }

}
