package com.su.Model;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Component
public class MessageToSend {

    private List<Message> messageList;

    public MessageToSend() {
        messageList = new ArrayList<>();
    }

    public void addMessage(Long chatId, String text) {
        messageList.add(new Message(chatId, text));
    }

    public void removeMessage(Message message) {
        Iterator messageListIterator = messageList.iterator();
        while (messageListIterator.hasNext()) {
            Message tmpMessage = (Message) messageListIterator.next();
            if (tmpMessage.equals(message)) {
                messageListIterator.remove();
            }
        }
    }

    public List<Message> getMessageList() {
        return messageList;
    }

    public void setMessageList(List<Message> messageList) {
        this.messageList = messageList;
    }
}
