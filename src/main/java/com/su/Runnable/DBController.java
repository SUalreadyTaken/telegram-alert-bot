package com.su.Runnable;

import com.su.Model.DBCommands;
import com.su.Service.PriceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DBController {

	private final DBCommands dbCommands;
	private final PriceDataService priceDataService;

	@Autowired
	public DBController(DBCommands dbCommands, PriceDataService priceDataService) {
		this.dbCommands = dbCommands;
		this.priceDataService = priceDataService;
	}

	@Scheduled(fixedDelay = 5000)
	public void run() {

		synchronized (dbCommands.getAddPriceToChatIdWatchlist()) {
			if (!dbCommands.getAddPriceToChatIdWatchlist().isEmpty()) {
				for (Map.Entry<Double, List<Integer>> doubleListEntry : dbCommands.getAddPriceToChatIdWatchlist().entrySet()) {
					for (Integer chatId : doubleListEntry.getValue()) {
						priceDataService.add(doubleListEntry.getKey(), chatId);
					}
				}
				dbCommands.getAddPriceToChatIdWatchlist().clear();
			}
		}

		synchronized (dbCommands.getRemovePriceFromChatIdWatchlist()) {
			if (!dbCommands.getRemovePriceFromChatIdWatchlist().isEmpty()) {
				for (Map.Entry<Double, List<Integer>> doubleListEntry : dbCommands.getRemovePriceFromChatIdWatchlist().entrySet()) {
					for (Integer chatId : doubleListEntry.getValue()) {
						priceDataService.removeChatId(doubleListEntry.getKey(), chatId);
					}
				}
				dbCommands.getRemovePriceFromChatIdWatchlist().clear();
			}
		}

		synchronized (dbCommands.getRemoveChatIdFromPrice()) {
			if (!dbCommands.getRemoveChatIdFromPrice().isEmpty()) {
				for (Map.Entry<Double, List<Integer>> doubleListEntry : dbCommands.getRemoveChatIdFromPrice().entrySet()) {
					priceDataService.removeChatIdFromPrice(doubleListEntry.getKey(), doubleListEntry.getValue());
				}
				dbCommands.getRemoveChatIdFromPrice().clear();
			}
		}

	}
}
