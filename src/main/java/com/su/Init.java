package com.su;

import com.su.Model.PriceData;
import com.su.Model.PriceWatchList;
import com.su.Repository.PriceDataRepository;
import com.su.Service.IdleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class Init {

	@Value("${switchapp}")
	private boolean switchApp;

	private final AlertBot alertBot;
	private final PriceDataRepository priceDataRepository;
	private final PriceWatchList priceWatchList;
	private final TelegramBotsApi botsApi;
	private final IdleService idleService;
	private BotSession botSession;
	private boolean running = true;

	@Autowired
	public Init(AlertBot alertBot, PriceDataRepository priceDataRepository, PriceWatchList priceWatchList, IdleService idleService) {
		this.alertBot = alertBot;
		this.priceDataRepository = priceDataRepository;
		this.priceWatchList = priceWatchList;
		this.idleService = idleService;
		this.botsApi = new TelegramBotsApi();
	}

	@PostConstruct
	private void start() {
		//TelegramBotsApi botsApi = new TelegramBotsApi();

		if (!switchApp || idleService.getAlternativeBoolean()) {
			//register telegram bot
			try {
				this.botSession = botsApi.registerBot(alertBot);
				System.out.println("Register bot success !\n" + "token >> " +
										   alertBot.getBotToken() + "\nusername >> " + alertBot.getBotUsername());
			} catch (TelegramApiException e) {
				e.printStackTrace();
			}

			List<PriceData> priceDataList = priceDataRepository.findAll();
			if (!priceDataList.isEmpty()) {
				for (PriceData priceData : priceDataList) {
					for (Integer chatId : priceData.getChatIds()) {
						this.priceWatchList.addChatIdToPrice(priceData.getPrice(), chatId);
					}
				}
			}
		}

	}

	void registerOrStop() {
		if (running) {
			System.out.println("bot is running going to stop it");
			botSession.stop();
		} else {
			System.out.println("bot is stopped will start it again");
			botSession.start();
		}
		running = !running;
	}
}
