package com.su.Model;

import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class MessageToSend {

  private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();

  public MessageToSend() {
  }

  public BlockingQueue<Message> getMessageQueue() {
    return messageQueue;
  }

  public void setMessageQueue(BlockingQueue<Message> messageQueue) {
    this.messageQueue = messageQueue;
  }

}
