package com.github.davidmoten.fsm.example.shop.catalog.event;

import com.github.davidmoten.fsm.example.shop.catalog.Catalog;
import com.github.davidmoten.fsm.runtime.Event;

public final class Create implements Event<Catalog> {

    public final String catalogId;
    public final String name;

    public Create( String catalogId,  String name) {
        this.catalogId = catalogId;
        this.name = name;
    }

}
