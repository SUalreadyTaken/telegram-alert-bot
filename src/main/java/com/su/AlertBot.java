package com.su;

import com.su.Model.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class AlertBot extends TelegramLongPollingBot {

  private final Price price;
  private final PriceWatchList priceWatchList;
  private final MessageToSend messageToSend;
  private final DBCommandsQueue dbCommandsQueue;

  @Value("${telegram.token}")
  private String token;

  @Value("${telegram.username}")
  private String username;

  @Autowired
  public AlertBot(Price price, PriceWatchList priceWatchList, MessageToSend messageToSend,
      DBCommandsQueue dbCommandsQueue) {
    this.price = price;
    this.priceWatchList = priceWatchList;
    this.messageToSend = messageToSend;
    this.dbCommandsQueue = dbCommandsQueue;
  }

  @Override
  public void onUpdateReceived(Update update) {

    if (update.hasMessage() && update.getMessage().hasText()) {

      System.out.println("Got this msg >> " + update.getMessage().getText());

      String command = update.getMessage().getText();
      int chatId = Math.toIntExact(update.getMessage().getChatId());
      StringBuilder respondMessage = new StringBuilder();

      if (command.startsWith("/start")) {
        double currentPrice = price.getPrice();
        respondMessage.append("Im online.. current btc price >> ").append(currentPrice);
        System.out.println("Im online.. current btc price >> " + currentPrice);
      }

      // yes works with chars added to command end.. /watchlistfoo
      if (command.startsWith("/watchlist") || command.startsWith("/add") || command.startsWith("/remove")
          || command.startsWith("/edit")) {
        Map<Double, List<Integer>> tmpPriceWatchlist = priceWatchList.getPrices();
        synchronized (priceWatchList.getPrices()) {
          if (command.startsWith("/watchlist")) {
            getUserWatchlist(chatId, respondMessage, tmpPriceWatchlist);
          } else {
            String[] commandStringArray = trimCommand(command);
            String commandString = commandStringArray[0];
            List<String> commandPrices = Stream.of(commandStringArray).skip(1).collect(Collectors.toList());
            if (!commandPrices.isEmpty()) {
              switch (commandString) {
                case "/add":
                  addToWatchlist(tmpPriceWatchlist, commandPrices, chatId, respondMessage);
                  break;
                case "/remove":
                  removeFromWatchlist(tmpPriceWatchlist, commandPrices, chatId, respondMessage);
                  break;
                case "/edit":
                  if (commandPrices.size() == 2) {
                    removeFromWatchlist(tmpPriceWatchlist, commandPrices.subList(0, 1), chatId, respondMessage);
                    if (respondMessage.toString().isEmpty()) {
                      respondMessage.append("Nothing to remove\n");
                    }
                    addToWatchlist(tmpPriceWatchlist, commandPrices.subList(1, 2), chatId, respondMessage);
                  } else {
                    respondMessage.append("Invalid command.. example /edit 1000.5 1050 ");
                  }
                  break;
              }
            } else {
              respondMessage.append("Invalid command.. example /add 3500 or /remove 3500 or /edit 3500 3300");
            }

          }

        }

      }

      if (!respondMessage.toString().isEmpty()) {
        try {
          messageToSend.getMessageQueue().put(new Message(chatId, respondMessage.toString()));
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      } else {
        System.out.println("Respond message is empty");
      }

    }

  }

  private void getUserWatchlist(int chatId, StringBuilder respondMessage,
      Map<Double, List<Integer>> tmpPriceWatchlist) {
    List<Double> unsortedWatchlist = new ArrayList<>();
    for (Map.Entry<Double, List<Integer>> watchlistPriceSet : tmpPriceWatchlist.entrySet()) {
      List<Integer> tmpChatIds = watchlistPriceSet.getValue();
      if (!tmpChatIds.isEmpty() && tmpChatIds.contains(chatId)) {
        unsortedWatchlist.add(watchlistPriceSet.getKey());
      }
    }
    unsortedWatchlist.sort(Collections.reverseOrder());
    for (Double watchListPrice : unsortedWatchlist) {
      respondMessage.append(watchListPrice).append("\n");
    }
    System.out.println(
        "*Watchlist* sending watchlist to " + chatId + " watchlist is as follows \n" + respondMessage.toString());
  }

  private String[] trimCommand(String command) {
    // replace multiple spaces with 1, split by spaces
    return command.trim().replaceAll("\\s+", " ").split(" ");
  }

  private void addToWatchlist(Map<Double, List<Integer>> watchList, List<String> priceList, int chatId,
      StringBuilder respondMessage) {
    
    priceList.stream()
      .filter(s -> isNumber(s, respondMessage))
      .map(Double::valueOf)
      .filter(d -> isAboveZero(d, respondMessage))
      .filter(d -> endsCorrectly(d, respondMessage))
      .filter(d -> isBetweenRange(d, respondMessage))
      .forEach(d -> {
        if (watchList.containsKey(d)) {
          // no duplicates to chatId list
          if (!watchList.get(d).contains(chatId)) {
            add(chatId, d);
            respondMessage.append("Added ").append(d).append(" to watchlist \n");
          } else {
            respondMessage.append(d).append(" already in watchlist \n");
          }
        } else {
          add(chatId, d);
          respondMessage.append("Added ").append(d).append(" to watchlist \n");
        }
      });
  }

  private void removeFromWatchlist(Map<Double, List<Integer>> watchList, List<String> priceList, int chatId,
      StringBuilder respondMessage) {
    
    priceList.stream()
      .filter(s -> isNumber(s, respondMessage))
      .map(Double::valueOf)
      .filter(d -> isAboveZero(d, respondMessage))
      .forEach(d -> {
        if (watchList.containsKey(d) && watchList.get(d).contains(chatId)) {
          remove(chatId, d);
          respondMessage.append("Removed ").append(d).append(" from watchlist\n");
        }
      });
  }

  private boolean isNumber(String s, StringBuilder respondMessage) {
    if (NumberUtils.isParsable(s))
      return true;
    respondMessage.append("[").append(s).append("]").append(" not a number (must use . instead of , )\n");
    return false;

  }

  private boolean isAboveZero(double d, StringBuilder respondMessage) {
    if (d > 0) 
      return true;
    respondMessage.append(d).append("can't be negative nubmer \n");
    return false;
  }

  private boolean endsCorrectly(double d, StringBuilder respondMessage) {
    if (d % 1 == 0 || (Math.round(d * 10) / 10.0) % .5 == 0) 
      return true;
    respondMessage.append("[").append(d).append("]").append(" must be exact number or end with .5\n");
    return false;
  }

  private boolean isBetweenRange(double d, StringBuilder respondMessage) {
    if (d > 0 && price.getPrice() * 2 >= d) 
      return true;
    respondMessage
      .append("[")
      .append(d)
      .append("]")
      .append(" out of range 0 - ")
      .append(price.getPrice() * 2)
      .append("\n");
    return false;
  }

  private void add(int chatId, Double price) {
    this.priceWatchList.addChatIdToPrice(price, chatId);
    try {
      this.dbCommandsQueue
        .getDbCommandsQueue()
        .put(new DBCommand(DBCommandType.ADDPRICE, price, Collections.singletonList(chatId)));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    System.out.println("*Add* added " + price + " to " + chatId + " watchlist");
  }

  private void remove(int chatId, Double price) {
    this.priceWatchList.removeChatIdFromPrice(chatId, price);
    try {
      this.dbCommandsQueue
        .getDbCommandsQueue()
        .put(new DBCommand(DBCommandType.REMOVEPRICE, price, Collections.singletonList(chatId)));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    System.out.println("*Remove* Removed " + price + " from " + chatId + " watchlist");
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
