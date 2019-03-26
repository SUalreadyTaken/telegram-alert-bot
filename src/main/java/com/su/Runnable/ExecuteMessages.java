package com.su.Runnable;

import com.su.AlertBot;
import com.su.Model.Message;
import com.su.Model.MessageToSend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Iterator;

@Component
public class ExecuteMessages implements Runnable {

    private MessageToSend messageToSend;
    private AlertBot alertBot;
    private int MESSAGES_SENT = 0;

    @Autowired
    public ExecuteMessages(MessageToSend messageToSend, AlertBot alertBot) {
        this.messageToSend = messageToSend;
        this.alertBot = alertBot;
    }

    @Override
    public void run() {
        if (!messageToSend.getMessageList().isEmpty()) {
            Iterator messageListIterator = messageToSend.getMessageList().iterator();
            SendMessage sendMessage = new SendMessage();
            while (messageListIterator.hasNext()) {
                Message tmpMessage = (Message) messageListIterator.next();
                sendMessage.setChatId(tmpMessage.getChatId());
                sendMessage.setText(tmpMessage.getText());
                try {
                    alertBot.execute(sendMessage);
                    System.out.println("Sending message >> " + sendMessage.getText() + " to " + sendMessage.getChatId());
                    MESSAGES_SENT++;
                    // can send only 30 messages per second so just sleep for 1.2sec after 30 messages
                    // todo check how many messages have been sent in the last second so you dont have to sleep randomly.. only when spam occurs
                    if (MESSAGES_SENT == 30) {
                        Thread.sleep(1200);
                        MESSAGES_SENT = 0;
                    }
                    messageListIterator.remove();
                } catch (TelegramApiException | InterruptedException e) {
                    System.out.println("error in ExecuteMessages");
                    e.printStackTrace();
                }
            }
        }
    }

}
