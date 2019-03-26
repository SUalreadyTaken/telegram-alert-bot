package com.su.Model;

import org.springframework.stereotype.Component;

@Component
public class Price {

    private double price;

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}
