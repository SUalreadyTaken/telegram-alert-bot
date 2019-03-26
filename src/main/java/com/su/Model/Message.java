package com.su.Model;

public class Message {

    private long chatId;
    private String text;

    public Message(long chatId, String text) {
        this.chatId = chatId;
        this.text = text;
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
