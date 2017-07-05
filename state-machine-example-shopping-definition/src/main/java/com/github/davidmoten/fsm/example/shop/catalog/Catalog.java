package com.github.davidmoten.fsm.example.shop.catalog;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class Catalog {

    public final String catalogId;
    public final String name;

    @JsonCreator
    public Catalog(@JsonProperty("catalogId") String catalogId, @JsonProperty("name") String name) {
        this.catalogId = catalogId;
        this.name = name;
    }
}
