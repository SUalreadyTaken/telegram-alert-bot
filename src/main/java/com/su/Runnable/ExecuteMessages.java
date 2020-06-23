package com.su.Runnable;

import com.su.AlertBot;
import com.su.Model.Message;
import com.su.Model.MessageToSend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.concurrent.TimeUnit;

@Component
public class ExecuteMessages implements Runnable {

  private final MessageToSend messageToSend;
  private final AlertBot alertBot;
  private int MESSAGES_SENT = 0;
  private long LAST_MESSAGE_SENT = System.currentTimeMillis();

  @Autowired
  public ExecuteMessages(MessageToSend messageToSend, AlertBot alertBot) {
    this.messageToSend = messageToSend;
    this.alertBot = alertBot;
  }

  @Override
  public void run() {
    while (true) {
      try {
        Message message = messageToSend.getMessageQueue().take();
        SendMessage sendMessage = new SendMessage(message.getChatId(), message.getText());
        alertBot.execute(sendMessage);
        sleepIfNeeded();
      } catch (InterruptedException | TelegramApiException e) {
        e.printStackTrace();
      }
      LAST_MESSAGE_SENT = System.currentTimeMillis();
    }
  }

  public void sleepIfNeeded() {
    MESSAGES_SENT++;
    if (MESSAGES_SENT >= 30) {
      try {
        TimeUnit.MILLISECONDS.sleep(1000);
        MESSAGES_SENT = 0;
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    } else if (System.currentTimeMillis() - LAST_MESSAGE_SENT >= 1000) {
      MESSAGES_SENT = 1;
    }
  }

}
