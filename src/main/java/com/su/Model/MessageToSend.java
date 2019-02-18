package com.su.Model;

public class MessageToSend {

    public MessageToSend() {}

    public MessageToSend(Long chatId, String message) {
        this.chatId = chatId;
        this.message = message;
    }

    private long chatId;

    private String message;

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
