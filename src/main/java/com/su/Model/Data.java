package com.su.Model;

import java.util.List;

public class Data {

    private double price;

    private List<Long> chatIds;

    public Data () {}

    public Data (Double price, List<Long> chatIds) {
        this.price = price;
        this.chatIds = chatIds;
    }

    public List<Long> getChatIds() {
        return chatIds;
    }

    public void setChatIds(List<Long> chatIds) {
        this.chatIds = chatIds;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }


}
