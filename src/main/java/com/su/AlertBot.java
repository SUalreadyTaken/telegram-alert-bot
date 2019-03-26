package com.su;

import com.su.Model.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Component
public class AlertBot extends TelegramLongPollingBot {

    private Price price;
    private PriceWatchList priceWatchList;
    private MessageToSend messageToSend;

    @Value("${telegram.token}")
    private String token;

    @Value("${telegram.username}")
    private String username;

    @Autowired
    public AlertBot(Price price, PriceWatchList priceWatchList, MessageToSend messageToSend) {
        this.price = price;
        this.priceWatchList = priceWatchList;
        this.messageToSend = messageToSend;
    }

    public AlertBot() {
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {

            System.out.println("Got this msg >> " + update.getMessage().getText());

            String command = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (command.startsWith("/start")) {
                messageToSend.addMessage(chatId, "Im online.. current btc price >> " + price.getPrice());
            }

            if (command.startsWith("/price")) {
                System.out.println("ChatId " + chatId + " asked for btc price");
                messageToSend.addMessage(chatId, "Bitmex btc = " + price.getPrice());

            }

            if (command.startsWith("/watchlist")) {
                StringBuilder stringBuilder = new StringBuilder();
                for (double tmpPrice : priceWatchList.getPrices().keySet()) {
                    List<Long> tmpChatIds = priceWatchList.getPrices().get(tmpPrice);
                    if (!tmpChatIds.isEmpty()) {
                        if (tmpChatIds.contains(chatId)) {
                            stringBuilder.append(tmpPrice).append("\n");
                        }
                    }
                }
                System.out.println("*Watchlist* sending watchlist to " + chatId + " watchlist is as follows \n" + stringBuilder.toString());
                if (!stringBuilder.toString().isEmpty()) {
                    messageToSend.addMessage(chatId, stringBuilder.toString());
                }

            }

            if (command.startsWith("/add")) {
                String[] cmdString = command.split(" ");
                if (cmdString.length == 2) {
                    if (NumberUtils.isParsable(cmdString[1]) && Double.valueOf(cmdString[1]) > 0) {
                        Double tmpPrice = Double.valueOf(cmdString[1]);
                        if (tmpPrice % 1 == 0 || (Math.round(tmpPrice * 10) / 10.0) % .5 == 0) {
                            if (isBetween(price.getPrice() - 1000, price.getPrice() + 1000, tmpPrice)) {
                                if (priceWatchList.getPrices().containsKey(tmpPrice)) {
                                    if (!priceWatchList.getPrices().get(tmpPrice).contains(chatId)) {
                                        add(chatId, tmpPrice);
                                    }
                                } else {
                                    add(chatId, tmpPrice);
                                }
                            } else {
                                messageToSend.addMessage(chatId, "Price out of range +/- 1000 of current price");
                            }
                        } else {
                            messageToSend.addMessage((chatId), "Number must be whole or end with .5");
                        }
                    } else {
                        messageToSend.addMessage(chatId, "Price not a number or is negative");
                    }
                } else {
                    messageToSend.addMessage(chatId, "Invalid command.. example /add 3500.5");
                }

            }

            if (command.startsWith("/remove")) {
                String[] cmdString = command.split(" ");
                if (cmdString.length == 2) {
                    if (StringUtils.isNumeric(cmdString[1]) && Double.valueOf(cmdString[1]) > 0) {
                        Double tmpPrice = Double.valueOf(command.split(" ")[1]);
                        if (priceWatchList.getPrices().containsKey(tmpPrice) && priceWatchList.getPrices().get(tmpPrice).contains(chatId)) {
                            remove(chatId, tmpPrice);
                        }
                    } else {
                        messageToSend.addMessage(chatId, "Price not a number or is negative");
                    }
                } else {
                    messageToSend.addMessage(chatId, "Invalid command.. example /remove 3500");
                }
            }

        }

    }

    // return true if c i between a and b
    private boolean isBetween(double a, double b, double c) {
        return b > a ? c > a && c < b : c > b && c < a;
    }

    private void add(Long chatId, Double price) {
        this.priceWatchList.addChatIdToPrice(price, chatId);
        System.out.println("*Add* added " + chatId + " to " + price + " send alert map value");
        messageToSend.addMessage(chatId, "Added " + price + " to watchlist");
    }

    private void remove(Long chatId, Double price) {
        this.priceWatchList.removeChatIdFromPrice(chatId, price);
        System.out.println("*Remove* Removed " + chatId + " from " + price + " watchlist value list");
        messageToSend.addMessage(chatId, "Removed " + price + " from watchlist");
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }


}
