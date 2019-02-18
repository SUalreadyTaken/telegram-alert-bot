package com.su;

import com.su.Model.ChatWatchList;
import com.su.Model.MessageToSend;
import com.su.Model.Price;
import com.su.Model.PriceWatchList;
import com.su.Runnable.CheckPrice;
import com.su.Runnable.ExecuteMessages;
import com.su.Runnable.PreventIdling;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Controller
@SpringBootApplication
public class Main {

  public static void main(String[] args) {
    // needs to be before run..
    ApiContextInitializer.init();
    SpringApplication.run(Main.class, args);

    AnnotationConfigApplicationContext context =
            new AnnotationConfigApplicationContext(AlertBot.class, PreventIdling.class, ExecuteMessages.class, CheckPrice.class);

    AlertBot alertBot = context.getBean("alertBot", AlertBot.class);
    PreventIdling preventIdling = context.getBean("preventIdling", PreventIdling.class);
    ExecuteMessages executeMessages = context.getBean("executeMessages", ExecuteMessages.class);
    CheckPrice checkPrice = context.getBean("checkPrice", CheckPrice.class);

    TelegramBotsApi botsApi = new TelegramBotsApi();
    PriceWatchList priceWatchList = new PriceWatchList();
    ChatWatchList chatWatchList = new ChatWatchList();
    Price price = new Price();
    List<MessageToSend> messageToSendList = new ArrayList<>();

    //register telegram bot and set pointers
    try {
      botsApi.registerBot(alertBot);

      alertBot.setPriceWatchList(priceWatchList);
      alertBot.setChatWatchList(chatWatchList);
      alertBot.setPrice(price);
      alertBot.setMessageToSendList(messageToSendList);

      executeMessages.setMessageToSendList(messageToSendList);
      executeMessages.setAlertBot(alertBot);

      checkPrice.setAlertBot(alertBot);
      checkPrice.setMessageToSendList(messageToSendList);
      checkPrice.setPrice(price);

      System.out.println("Register bot success !\n" + "token >> " +
              alertBot.getBotToken() + "\nusername >> " + alertBot.getBotUsername());
    } catch (TelegramApiException e) {
      e.printStackTrace();
    }

    // bitmex not logged in users request limit is 150 per 3 min.. so check every 3 sec to be safe.. min 1.2sec
    ScheduledExecutorService checkPriceExecutor = Executors.newScheduledThreadPool(1);
    checkPriceExecutor.scheduleAtFixedRate(checkPrice, 0, 3, TimeUnit.SECONDS);

    // send out messages
    ScheduledExecutorService executeMessageExecutor = Executors.newScheduledThreadPool(1);
    executeMessageExecutor.scheduleAtFixedRate(executeMessages, 0, 1, TimeUnit.SECONDS);

    // ping your heroku app to keep it from going to sleep
    ScheduledExecutorService preventIdlingExecutor = Executors.newScheduledThreadPool(1);
    preventIdlingExecutor.scheduleAtFixedRate(preventIdling, 0, 5, TimeUnit.MINUTES);


  }
  @RequestMapping("/")
  String index() {
    return "index";
  }


}
