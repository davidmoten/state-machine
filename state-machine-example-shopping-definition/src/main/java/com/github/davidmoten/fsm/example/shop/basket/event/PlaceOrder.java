package com.github.davidmoten.fsm.example.shop.basket.event;

public final class PlaceOrder {
    public final String address;
    public final String phoneNumber;

    public PlaceOrder(String address, String phoneNumber) {
        this.address = address;
        this.phoneNumber = phoneNumber;
    }

}
