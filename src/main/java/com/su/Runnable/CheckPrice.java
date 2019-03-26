package com.su.Runnable;

import com.su.Model.MessageToSend;
import com.su.Model.Price;
import com.su.Model.PriceWatchList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Component
public class CheckPrice implements Runnable {

    @Value("${bitmex}")
    private String MEX;

    private int EVERY_MINUTE = 0;
    private double PRICE = 0;

    private Price price;
    private MessageToSend messageToSend;
    private PriceWatchList priceWatchList;

    @Autowired
    public CheckPrice(Price price, MessageToSend messageToSend, PriceWatchList priceWatchList) {
        this.price = price;
        this.messageToSend = messageToSend;
        this.priceWatchList = priceWatchList;
    }


    @Override
    public void run() {
        try {
            URL url = new URL(MEX);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            String JSON_STRING = new Scanner(connection.getInputStream(), "UTF-8").next();

            JSONArray jsonArray = new JSONArray(JSON_STRING);
            JSONObject jsonObject = (JSONObject) jsonArray.get(0);

            double newPrice = Double.parseDouble(jsonObject.get("price").toString());

            // 3 * EVERY_MINUTE to remind what price is
            EVERY_MINUTE++;
            if (EVERY_MINUTE == 20) {
                System.out.println("Reminder Current price >> " + newPrice);
                EVERY_MINUTE = 0;
            }

            // price changed
            // check if i need to send an alert
            if (newPrice != PRICE) {

                System.out.println("Got new price.. will set it >> " + newPrice);
                price.setPrice(newPrice);

                Iterator priceListIterator = priceWatchList.getPrices().keySet().iterator();
                // go through watchList
                while (priceListIterator.hasNext()) {
                    Double tmpPrice = (Double) priceListIterator.next();
                    // if price on watchlist equals or is between current and old btc price
                    if (Objects.equals(tmpPrice, newPrice) || (isBetween(newPrice, PRICE, tmpPrice) && PRICE != 0)) {
                        System.out.println("NEED to send alert on price >> " + newPrice);
                        // add all chatIds to sendList
                        for (Long chatId : priceWatchList.getPrices().get(tmpPrice)) {
                            String text;
                            if (PRICE > newPrice) {
                                text = "ALERT price fell below >> " + tmpPrice;
                            } else {
                                text = "ALERT price rose above >> " + tmpPrice;
                            }
                            System.out.println("Sending alert to " + chatId + " with text >> " + text);
                            messageToSend.addMessage(chatId, text);
                        }
                        priceListIterator.remove();
                    }
                }
            }

            PRICE = newPrice;
            connection.disconnect();

        } catch (IOException ex) {
            System.out.println("Error in CheckPrice");
            ex.printStackTrace();
        }
    }

    private boolean isBetween(double a, double b, double c) {
        return b > a ? c > a && c < b : c > b && c < a;
    }
}
