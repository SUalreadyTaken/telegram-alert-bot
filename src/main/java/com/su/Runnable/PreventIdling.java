package com.su.Runnable;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Ping heroku app to prevent it from going to sleep
 */
@Component
public class PreventIdling implements Runnable {

    @Value("${heroku.website}")
    private String herokuWebsite;

    @Override
    public void run() {
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
