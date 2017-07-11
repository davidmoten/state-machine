package com.github.davidmoten.fsm.example.shop.catalogproduct.event;

import java.util.List;

import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
import com.github.davidmoten.fsm.runtime.Event;

public final class ChangeProductDetails implements Event<CatalogProduct> {

    public final String productName;
    public final String productDescription;
    public final List<String> tags;

    public ChangeProductDetails(String productName, String productDescription, List<String> tags) {
        this.productName = productName;
        this.productDescription = productDescription;
        this.tags = tags;
    }

}
