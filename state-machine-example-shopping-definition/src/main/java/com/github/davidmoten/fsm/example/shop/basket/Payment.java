package com.github.davidmoten.fsm.example.shop.basket;

import com.github.davidmoten.bean.annotation.GenerateImmutable;
import com.github.davidmoten.fsm.example.shop.customer.Customer;
import com.github.davidmoten.fsm.runtime.Event;

@GenerateImmutable
public class Payment implements Event<Customer>{

}
