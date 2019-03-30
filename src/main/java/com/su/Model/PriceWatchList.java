package com.su.Model;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Component
public class PriceWatchList {


    private Map<Double, List<Long>> prices = new LinkedHashMap<>();

    public Map<Double, List<Long>> getPrices() {
        return prices;
    }

    public void setPrices(Map<Double, List<Long>> prices) {
        this.prices = prices;
    }

    public void addChatIdToPrice(Double price, Long chatId) {
        if (this.prices == null) {
            this.prices = new LinkedHashMap<>();
        }
        // does the map have this key
        if (this.prices.containsKey(price)) {
            // chatId not in list
            if (!this.prices.get(price).contains(chatId)) {
                this.prices.get(price).add(chatId);
            }
        } else {
            this.prices.put(price, Stream.of(chatId).collect(Collectors.toList()));
        }

    }

    public void removeChatIdFromPrice(Long chatId, Double price) {
        if (this.prices != null) {
            if (this.prices.containsKey(price)) {
                Iterator chatIdIterator = this.prices.get(price).iterator();
                Long tmpChatId;
                while (chatIdIterator.hasNext()) {
                    tmpChatId = (Long) chatIdIterator.next();
                    if (Objects.equals(tmpChatId, chatId)) {
                        chatIdIterator.remove();
                        break;
                    }
                }
                // remove price.. wont be kept with empty chatId list
                if (this.prices.get(price).isEmpty()) {
                    this.prices.remove(price);
                }
            }
        }
    }

}
