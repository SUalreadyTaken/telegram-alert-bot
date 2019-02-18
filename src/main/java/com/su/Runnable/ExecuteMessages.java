package com.su.Runnable;

import com.su.AlertBot;
import com.su.Model.MessageToSend;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
public class ExecuteMessages implements Runnable {

    private List<MessageToSend> messageToSendList = new ArrayList<>();
    private AlertBot alertBot;
    private int MESSAGES_SENT = 0;

    @Override
    public void run() {
        if (!messageToSendList.isEmpty()) {
            System.out.println("Have some messages to send");
            Iterator messagesToSendListIterator = messageToSendList.iterator();
            SendMessage sendMessage = new SendMessage();
            MessageToSend messageToSend;
            while (messagesToSendListIterator.hasNext()) {
                messageToSend = (MessageToSend) messagesToSendListIterator.next();
                sendMessage.setChatId(messageToSend.getChatId());
                sendMessage.setText(messageToSend.getMessage());
                try {
                    alertBot.execute(sendMessage);
                    System.out.println("Sending message >> " + sendMessage.toString());
                    MESSAGES_SENT++;
                    // can send only 30 messages per second so just sleep for 1.2sec after doing so
                    // todo check how many messages have been sent in the last second so you dont have to sleep randomly.. only when spam occurs
                    if (MESSAGES_SENT == 30) {
                        Thread.sleep(1200);
                        MESSAGES_SENT = 0;
                    }
                    messagesToSendListIterator.remove();
                } catch (TelegramApiException | InterruptedException e) {
                    System.out.println("error in ExecuteMessages");
                    e.printStackTrace();
                }
            }
        }
    }

    public AlertBot getAlertBot() {
        return alertBot;
    }

    public void setAlertBot(AlertBot alertBot) {
        this.alertBot = alertBot;
    }

    public List<MessageToSend> getMessageToSendList() {
        return messageToSendList;
    }

    public void setMessageToSendList(List<MessageToSend> messageToSendList) {
        this.messageToSendList = messageToSendList;
    }

}
