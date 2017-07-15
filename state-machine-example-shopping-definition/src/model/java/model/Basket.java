package model;

import java.util.ArrayList;
import java.util.List;

import com.github.davidmoten.bean.annotation.ImmutableBean;

@ImmutableBean
public final class Basket {

    public final String id;
    public final List<BasketProduct> items;

    public Basket(String id, List<BasketProduct> items) {
        this.id = id;
        this.items = new ArrayList<>(items);
    }

}
