package com.su.Runnable;

import com.su.Service.IdleService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Ping heroku app to prevent it from going to sleep
 */
@Component
public class PreventIdling {

    @Value("${heroku.website}")
    private String herokuWebsite;

    @Value("${switchapp}")
    private boolean switchApp;

    private final IdleService idleService;
    private boolean alternative;

    public PreventIdling(IdleService idleService) {
        this.idleService = idleService;
    }

    @PostConstruct
    private void setAlternative() {
        if (switchApp) {
            alternative = this.idleService.getAlternativeBoolean();
        }
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void run() {
        //if this app is the main one then alternative be false to keep it from idling.. vice versa for the alternative app
        //if switchApp is false it will keep the app from idling
        if (!switchApp || !alternative) {
            HttpURLConnection connection = null;
            try {
                URL u = new URL(herokuWebsite);
                connection = (HttpURLConnection) u.openConnection();
                connection.setRequestMethod("HEAD");
                int code = connection.getResponseCode();
                System.out.println("My website header code >> " + code);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }
}
