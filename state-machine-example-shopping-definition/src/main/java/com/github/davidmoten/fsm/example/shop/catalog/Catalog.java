package com.github.davidmoten.fsm.example.shop.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Catalog {

    public final String catalogId;
    public final String name;

    public Catalog( String catalogId,  String name) {
        this.catalogId = catalogId;
        this.name = name;
    }
    
}
