package com.su;

import com.su.Model.ChatWatchList;
import com.su.Model.MessageToSend;
import com.su.Model.Price;
import com.su.Model.PriceWatchList;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Component
@PropertySource(value = "classpath:telegram.properties")
public class AlertBot extends TelegramLongPollingBot {

    private Price price;
    private PriceWatchList priceWatchList;
    private ChatWatchList chatWatchList;
    private List<MessageToSend> messageToSendList;

    @Value("${token}")
    private String token;

    @Value("${username}")
    private String username;

    public void setPrice(Price price) {
        this.price = price;
    }

    public Price getPrice() {
        return this.price;
    }

    public PriceWatchList getPriceWatchList() {
        return priceWatchList;
    }

    public void setPriceWatchList(PriceWatchList priceWatchList) {
        this.priceWatchList = priceWatchList;
    }

    public ChatWatchList getChatWatchList() {
        return chatWatchList;
    }

    public void setChatWatchList(ChatWatchList chatWatchList) {
        this.chatWatchList = chatWatchList;
    }

    public void setMessageToSendList(List<MessageToSend> messageToSendList) {
        this.messageToSendList = messageToSendList;
    }

    public List<MessageToSend> getMessageToSendList() {
        return messageToSendList;
    }


    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {

            System.out.println("Got this msg >> " + update.getMessage().getText());

            String command = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (command.startsWith("/start")) {
                messageToSendList.add(new MessageToSend(chatId, "Im online.. current btc price >> " + price.getPrice()));
            }

            if (command.startsWith("/price")) {
                System.out.println("ChatId " + chatId + " asked for btc price");
                messageToSendList.add(new MessageToSend(chatId, "Bitmex btc = " + price.getPrice()));

            }

            if (command.startsWith("/watchlist")) {
                StringBuilder stringBuilder = new StringBuilder();
                if (this.chatWatchList.getChatWatchList().containsKey(chatId)) {
                    for (Double d: this.chatWatchList.getChatWatchList().get(chatId)) {
                        stringBuilder.append(d + "\n");
                    }
                }
                System.out.println("*Watchlist* sending watchlist to " + chatId + " watchlist is as follows \n" + stringBuilder.toString());
                if (!stringBuilder.toString().isEmpty()) {
                    messageToSendList.add(new MessageToSend(chatId, stringBuilder.toString()));
                }

            }

            if (command.startsWith("/add"))  {
                String[] cmdString = command.split(" ");
                if (cmdString.length == 2) {
                    if (NumberUtils.isParsable(cmdString[1]) && Double.valueOf(cmdString[1]) > 0) {
                        Double tmpPrice = Double.valueOf(cmdString[1]);
                        if(tmpPrice % 1 == 0 || (Math.round(tmpPrice * 10) / 10.0) % .5 == 0) {
                            if (isBetween(price.getPrice() - 1000, price.getPrice() + 1000, tmpPrice)) {
                                this.chatWatchList.addPriceToChatId(chatId, tmpPrice);
                                System.out.println("*Add* added " + tmpPrice + " to " + chatId + " chat watchlist");
                                this.priceWatchList.addChatIdToPrice(tmpPrice, chatId);
                                System.out.println("*Add* added " + chatId + " to " + tmpPrice + " send alert map value");
                                messageToSendList.add(new MessageToSend(chatId, "Added " + tmpPrice + " to watchlist"));
                            } else {
                                messageToSendList.add(new MessageToSend(chatId, "Price out of range +/- 1000 of current price"));
                            }
                        } else {
                            messageToSendList.add(new MessageToSend((chatId), "Number must be whole or end with .5"));
                        }
                    } else {
                        messageToSendList.add(new MessageToSend(chatId, "Price not a number or is negative"));
                    }
                } else {
                    messageToSendList.add(new MessageToSend(chatId, "Invalid command.. example /add 3500.5"));
                }

            }

            if (command.startsWith("/remove"))  {
                String[] cmdString = command.split(" ");
                if (cmdString.length == 2) {
                    if (StringUtils.isNumeric(cmdString[1]) && Double.valueOf(cmdString[1]) > 0) {
                        Double tmpPrice = Double.valueOf(command.split(" ")[1]);
                        this.chatWatchList.removePriceFromChatId(chatId, tmpPrice);
                        System.out.println("*Remove* Removed " + tmpPrice + " from " + chatId + " chatWatchList");

                        this.priceWatchList.removeChatIdFromPrice(chatId, tmpPrice);
                        System.out.println("*Remove* Removed " + chatId + " from " + tmpPrice + " watchlist value list");

                        messageToSendList.add(new MessageToSend(chatId, "Removed " + tmpPrice + " from watchlist"));
                    } else {
                        messageToSendList.add(new MessageToSend(chatId, "Price not a number or is negative"));
                    }
                } else {
                    messageToSendList.add(new MessageToSend(chatId, "Invalid command.. example /remove 3500"));
                }
            }

        }

    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    // return true if c i between a and b
    private boolean isBetween(double a, double b, double c) {
        return b > a ? c > a && c < b : c > b && c < a;
    }


}
