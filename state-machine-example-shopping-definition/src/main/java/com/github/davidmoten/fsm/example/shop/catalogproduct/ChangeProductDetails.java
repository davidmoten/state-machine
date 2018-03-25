package com.github.davidmoten.fsm.example.shop.catalogproduct;

import java.util.List;

import com.github.davidmoten.bean.annotation.GenerateImmutable;
import com.github.davidmoten.fsm.runtime.Event;

@GenerateImmutable
public class ChangeProductDetails implements Event<CatalogProduct> {

    String productName;
    String productDescription;
    List<String> tags;

}
