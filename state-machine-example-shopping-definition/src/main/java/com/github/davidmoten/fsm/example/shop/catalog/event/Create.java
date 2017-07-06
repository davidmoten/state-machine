package com.github.davidmoten.fsm.example.shop.catalog.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.davidmoten.fsm.example.shop.catalog.Catalog;
import com.github.davidmoten.fsm.runtime.Event;

public final class Create implements Event<Catalog> {

    public final String catalogId;
    public final String name;

    @JsonCreator
    public Create(@JsonProperty("catalogId") String catalogId, @JsonProperty("name") String name) {
        super();
        this.catalogId = catalogId;
        this.name = name;
    }

}
