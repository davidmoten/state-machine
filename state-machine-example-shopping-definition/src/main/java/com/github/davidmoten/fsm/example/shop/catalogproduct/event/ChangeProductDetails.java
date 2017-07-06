package com.github.davidmoten.fsm.example.shop.catalogproduct.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.davidmoten.fsm.example.shop.catalogproduct.CatalogProduct;
import com.github.davidmoten.fsm.runtime.Event;

public final class ChangeProductDetails implements Event<CatalogProduct> {

    public final String productName;
    public final String productDescription;

    @JsonCreator
    public ChangeProductDetails(@JsonProperty("productName") String productName,
            @JsonProperty("productDescription") String productDescription) {
        this.productName = productName;
        this.productDescription = productDescription;
    }

}
