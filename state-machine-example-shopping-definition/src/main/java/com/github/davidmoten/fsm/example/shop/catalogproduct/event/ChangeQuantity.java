package com.github.davidmoten.fsm.example.shop.catalogproduct.event;

import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
import com.github.davidmoten.fsm.runtime.Event;

public final class ChangeQuantity implements Event<CatalogProduct> {

    public final int quantityDelta;

    public ChangeQuantity(int quantityDelta) {
        this.quantityDelta = quantityDelta;
    }

    @Override
    public String toString() {
        return "ChangeQuantity [quantityDelta=" + quantityDelta + "]";
    }

}
