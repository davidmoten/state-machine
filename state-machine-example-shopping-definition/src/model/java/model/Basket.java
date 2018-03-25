package model;

import java.util.List;

import com.github.davidmoten.bean.annotation.ImmutableBean;

@ImmutableBean
public class Basket {

    String id;
    List<BasketProduct> items;

}
