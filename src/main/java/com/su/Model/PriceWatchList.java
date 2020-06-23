package com.su.Model;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class PriceWatchList {

  private Map<Double, List<Integer>> prices = Collections.synchronizedMap(new LinkedHashMap<>());

  public Map<Double, List<Integer>> getPrices() {
    return prices;
  }

  public void setPrices(Map<Double, List<Integer>> prices) {
    this.prices = prices;
  }

  public void addChatIdToPrice(Double price, Integer chatId) {
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

  public void removeChatIdFromPrice(Integer chatId, Double price) {
    if (this.prices != null) {
      if (this.prices.containsKey(price)) {
        Iterator chatIdIterator = this.prices.get(price).iterator();
        // int tmpChatId;
        while (chatIdIterator.hasNext()) {
          // tmpChatId = (int) chatIdIterator.next();
          if (Objects.equals(chatIdIterator.next(), chatId)) {
            chatIdIterator.remove();
            break;
          }
        }
        if (this.prices.get(price).isEmpty()) {
          this.prices.remove(price);
        }
      }
    }
  }

}
