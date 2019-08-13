package com.su.Model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "pricedata")
public class PriceData {

    @Id
    private String id;

    private double price;

    private List<Integer> chatid;

    public PriceData() {}

    public PriceData(Double price, List<Integer> chatIds) {
        this.price = price;
        this.chatid = chatIds;
    }

    public List<Integer> getChatIds() {
        return chatid;
    }

    public void setChatIds(List<Integer> chatIds) {
        this.chatid = chatIds;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }


}
