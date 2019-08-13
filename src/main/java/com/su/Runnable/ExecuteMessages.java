package com.su.Runnable;

import com.su.AlertBot;
import com.su.Model.Message;
import com.su.Model.MessageToSend;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;

@Component
public class ExecuteMessages {

	private final MessageToSend messageToSend;
	private final AlertBot alertBot;
	private int MESSAGES_SENT = 0;
	private long LAST_MESSAGE_SENT = System.currentTimeMillis();

	@Autowired
	public ExecuteMessages(MessageToSend messageToSend, AlertBot alertBot) {
		this.messageToSend = messageToSend;
		this.alertBot = alertBot;
	}

	@Scheduled(fixedDelay = 100)
	public void run() throws InterruptedException {
		if (!messageToSend.getMessageList().isEmpty()) {
			synchronized (messageToSend.getMessageList()) {
				Iterator messageListIterator = messageToSend.getMessageList().iterator();
				long startTime = getStartTime();
				while (messageListIterator.hasNext()) {
					//telegram bot message
					SendMessage sendMessage = new SendMessage();
					createMessage(messageListIterator, sendMessage);
					try {
						alertBot.execute(sendMessage);
					} catch (TelegramApiException e) {
						System.out.println("error in ExecuteMessages");
						e.printStackTrace();
					}
					startTime = sleepIfNeeded(startTime, sendMessage);
					messageListIterator.remove();
				}
				LAST_MESSAGE_SENT = System.currentTimeMillis();
			}
		}
	}

	public long sleepIfNeeded(long startTime, SendMessage sendMessage) throws InterruptedException {
		System.out.println("Sending message >> " + sendMessage.getText() + " to " + sendMessage.getChatId());
		MESSAGES_SENT++;
		long timeSpentSendingMessages = System.currentTimeMillis() - startTime;
		// Can only send 30 messages per second
		if (timeSpentSendingMessages >= 1000 && MESSAGES_SENT <= 30) {
			MESSAGES_SENT = 1;
			startTime = System.currentTimeMillis();
		}
		if (MESSAGES_SENT == 30) {
			// sleep until second passes
			TimeUnit.MILLISECONDS.sleep(1000 - timeSpentSendingMessages);
			startTime = System.currentTimeMillis();
			MESSAGES_SENT = 0;
		}
		return startTime;
	}

	private void createMessage(Iterator messageListIterator, SendMessage sendMessage) {
		Message tmpMessage = (Message) messageListIterator.next();
		sendMessage.setChatId(tmpMessage.getChatId());
		sendMessage.setText(tmpMessage.getText());
	}

	private long getStartTime() {
		long startTime;
		if (System.currentTimeMillis() - LAST_MESSAGE_SENT >= 1000) {
			startTime = System.currentTimeMillis();
			MESSAGES_SENT = 0;
		} else {
			startTime = LAST_MESSAGE_SENT;
		}
		return startTime;
	}

}
