package com.github.davidmoten.fsm.example.shop.catalogproduct;

import com.github.davidmoten.bean.annotation.GenerateImmutable;
import com.github.davidmoten.fsm.runtime.Event;

@GenerateImmutable
public final class ChangeQuantity implements Event<CatalogProduct> {

    int quantityDelta;
    
}
