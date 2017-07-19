package com.github.davidmoten.fsm.example.shop.catalogproduct;

import java.math.BigDecimal;

import com.github.davidmoten.bean.annotation.GenerateImmutable;
import com.github.davidmoten.fsm.runtime.Event;

@GenerateImmutable
public final class Create implements Event<CatalogProduct> {

    String catalogId;
    String productId;
    int quantity;
    BigDecimal price;

}
