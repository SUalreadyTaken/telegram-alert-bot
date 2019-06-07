package com.su;

import com.su.Helpers.JsonManagement;
import com.su.Runnable.CheckPrice;
import com.su.Runnable.ExecuteMessages;
import com.su.Runnable.PreventIdling;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class Init {

    private AlertBot alertBot;
    private CheckPrice checkPrice;
    private ExecuteMessages executeMessages;
    private PreventIdling preventIdling;
    private JsonManagement jsonManagement;

    @Autowired
    public Init(AlertBot alertBot, CheckPrice checkPrice, ExecuteMessages executeMessages,
                PreventIdling preventIdling, JsonManagement jsonManagement) {
        this.alertBot = alertBot;
        this.checkPrice = checkPrice;
        this.executeMessages = executeMessages;
        this.preventIdling = preventIdling;
        this.jsonManagement = jsonManagement;
    }

    public Init() {
    }

    @PostConstruct
    public void start() {
        TelegramBotsApi botsApi = new TelegramBotsApi();

        //register telegram bot
        try {
            botsApi.registerBot(alertBot);
            System.out.println("Register bot success !\n" + "token >> " +
                    alertBot.getBotToken() + "\nusername >> " + alertBot.getBotUsername());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        // read data from json to priceWatchList
        jsonManagement.priceWatchListSetup();

        // Can/Could use @Scheduled on each class..

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

}
