package com.github.davidmoten.fsm.example.shop.basket.event;

import com.github.davidmoten.fsm.example.shop.customer.Customer;
import com.github.davidmoten.fsm.runtime.Event;

public final class PlaceOrder implements Event<Customer>{
    public final String address;
    public final String phoneNumber;

    public PlaceOrder(String address, String phoneNumber) {
        this.address = address;
        this.phoneNumber = phoneNumber;
    }

}
