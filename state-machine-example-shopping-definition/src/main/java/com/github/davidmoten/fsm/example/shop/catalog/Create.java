package com.github.davidmoten.fsm.example.shop.catalog;

import com.github.davidmoten.bean.annotation.GenerateImmutable;
import com.github.davidmoten.fsm.runtime.Event;

@GenerateImmutable
public class Create implements Event<Catalog> {

    String catalogId;
    String name;

}
