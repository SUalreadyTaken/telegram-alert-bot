package com.su.Model;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MessageToSend {

    private List<Message> messageList = Collections.synchronizedList(new ArrayList<>());

    public MessageToSend() {}

    public void addMessage(int chatId, String text) {
        messageList.add(new Message(chatId, text));
    }

    public void removeMessage(Message message) {
        Iterator messageListIterator = messageList.iterator();
        while (messageListIterator.hasNext()) {
//            Message tmpMessage = (Message) messageListIterator.next();
            if (messageListIterator.next().equals(message)) {
                messageListIterator.remove();
                break;
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
