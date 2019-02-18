package com.su.Model;

import java.util.*;

/**
 * chat(users) watchlist.
 * For making easier /watchlist command
 *
 * chatId, list<price>
 *
 * Map<Long, List<Double>>
 */
public class ChatWatchList {

    private Map<Long, List<Double>> chatWatchList = new LinkedHashMap<Long, List<Double>>();

    public Map<Long, List<Double>> getChatWatchList() {
        return chatWatchList;
    }

    public void setChatWatchList(Map<Long, List<Double>> chatWatchList) {
        this.chatWatchList = chatWatchList;
    }

    public void addPriceToChatId(Long chatId, Double price) {
        if (this.chatWatchList == null) {
            this.chatWatchList = new LinkedHashMap<Long, List<Double>>();
        }

        // have added a price to watchlist before
        if (this.chatWatchList.containsKey(chatId)) {
            if (!this.chatWatchList.get(chatId).contains(price)) {
                this.chatWatchList.get(chatId).add(price);
            }
        } else {
            // hasn't used watchlist before or it was empty at some point .. put with array list containing the price
            this.chatWatchList.put(chatId, new ArrayList<Double>(Arrays.asList(price)));
        }
    }

    public void removePriceFromChatId(Long chatId, Double price) {
        if (this.chatWatchList != null) {
            if (this.chatWatchList.containsKey(chatId)) {
                Iterator priceIterator = this.chatWatchList.get(chatId).iterator();
                Double tmpDouble;
                while (priceIterator.hasNext()) {
                    tmpDouble = (Double) priceIterator.next();
                    if (Objects.equals(tmpDouble, price)) {
                        priceIterator.remove();
                        break;
                    }
                }

                // if that price was the last one in that chatIds watchlist .. remove the chatID from map
                if (this.chatWatchList.get(chatId).isEmpty()) {
                    this.chatWatchList.remove(chatId);
                }
            }
        }
    }


}
