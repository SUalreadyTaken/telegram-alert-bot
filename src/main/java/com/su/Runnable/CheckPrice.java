package com.su.Runnable;

import com.su.AlertBot;
import com.su.Model.MessageToSend;
import com.su.Model.Price;
import org.json.JSONArray;
import org.json.JSONObject;
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

    private String JSON_STRING;
    private int EVERY_MINUTE = 0;
    private double PRICE = 0;

    private Price price;
    private AlertBot alertBot;
    private List<MessageToSend> messageToSendList = new ArrayList<>();

    @Override
    public void run() {
        try {
            URL url = new URL(MEX);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            JSON_STRING = new Scanner(connection.getInputStream(), "UTF-8").next();

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

                Iterator watchListPriceIterator = alertBot.getPriceWatchList().getPrices().keySet().iterator();
                // go through watchList
                while (watchListPriceIterator.hasNext()) {
                    Double priceOnWatchList = (Double) watchListPriceIterator.next();
                    // if price on watchlist equals or is between current and old btc price
                    if (Objects.equals(priceOnWatchList, newPrice)
                            || (isBetween(newPrice, PRICE, priceOnWatchList) && PRICE != 0)) {
                        System.out.println("NEED to send alert on price >> " + newPrice);
                        // add all chatIds to sendList
                        for (Long chatId: alertBot.getPriceWatchList().getPrices().get(priceOnWatchList)) {
                            String messageString;
                            if (PRICE > newPrice) {
                                messageString = "ALERT price fell below >> " + priceOnWatchList;
                            } else {
                                messageString = "ALERT price rose above >> " + priceOnWatchList;
                            }
                            System.out.println("Sending alert to >> " + chatId + " with text >> " + messageString);
                            messageToSendList.add(new MessageToSend(chatId, messageString));

                            // removes the price from ChatWatchlist
                            alertBot.getChatWatchList().removePriceFromChatId(chatId,priceOnWatchList);
                        }
                        // all messages added to sendList.. no need for that price on watchlist..remove it
                        // removes key(price) from priceWatchList
                        watchListPriceIterator.remove();
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

    public Price getPrice() {
        return price;
    }

    public void setPrice(Price price) {
        this.price = price;
    }

    public AlertBot getAlertBot() {
        return alertBot;
    }

    public void setAlertBot(AlertBot alertBot) {
        this.alertBot = alertBot;
    }

    public List<MessageToSend> getMessageToSendList() {
        return messageToSendList;
    }

    public void setMessageToSendList(List<MessageToSend> messageToSendList) {
        this.messageToSendList = messageToSendList;
    }
}
