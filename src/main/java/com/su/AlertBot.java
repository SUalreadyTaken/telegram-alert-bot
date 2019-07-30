package com.su;

import com.su.Model.*;
import com.su.Service.PriceDataService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class AlertBot extends TelegramLongPollingBot {

	private final Price price;
	private final PriceWatchList priceWatchList;
	private final MessageToSend messageToSend;
	private final PriceDataService priceDataService;

	@Value("${telegram.token}")
	private String token;

	@Value("${telegram.username}")
	private String username;

	@Autowired
	public AlertBot(Price price, PriceWatchList priceWatchList, MessageToSend messageToSend, PriceDataService priceDataService) {
		this.price = price;
		this.priceWatchList = priceWatchList;
		this.messageToSend = messageToSend;
		this.priceDataService = priceDataService;
	}

	@Override
	public void onUpdateReceived(Update update) {

		if (update.hasMessage() && update.getMessage().hasText()) {

			System.out.println("Got this msg >> " + update.getMessage().getText());

			String command = update.getMessage().getText();
			int chatId = Math.toIntExact(update.getMessage().getChatId());
			StringBuilder respondMessage = new StringBuilder();

			if (command.startsWith("/start")) {
				respondMessage.append("Im online.. current btc price >> ").append(price.getPrice());
				System.out.println("Im online.. current btc price >> " + price.getPrice());
			}
			
			if (command.startsWith("/watchlist")) {
				List<Double> tmpWatchList = new ArrayList<>();
				for (double tmpPrice : priceWatchList.getPrices().keySet()) {
					List<Integer> tmpChatIds = priceWatchList.getPrices().get(tmpPrice);
					if (!tmpChatIds.isEmpty()) {
						if (tmpChatIds.contains(chatId)) {
							tmpWatchList.add(tmpPrice);
						}
					}
				}
				//sort in reverse order and append to string
				tmpWatchList.sort(Collections.reverseOrder());
				for (Double aDouble : tmpWatchList) {
					respondMessage.append(aDouble).append("\n");
				}
				System.out.println("*Watchlist* sending watchlist to " + chatId + " watchlist is as follows \n" + respondMessage.toString());
			}
			
			if (command.startsWith("/add")) {
				List<String> priceList = Stream.of(trimCommand(command)).skip(1).collect(Collectors.toList());
				addToWatchlist(priceList, chatId, respondMessage);
			}
			
			if (command.startsWith("/remove")) {
				List<String> priceList = Stream.of(trimCommand(command)).skip(1).collect(Collectors.toList());
				removeFromWatchlist(priceList, chatId, respondMessage);
			}
			
			if (command.startsWith("/edit")) {
				List<String> priceToEdit = Stream.of(trimCommand(command)).skip(1).collect(Collectors.toList());
				if (priceToEdit.size() == 2) {
					removeFromWatchlist(priceToEdit.subList(0, 1), chatId, respondMessage);
					if (respondMessage.toString().isEmpty()) {
						respondMessage.append("Nothing to remove\n");
					}
					addToWatchlist(priceToEdit.subList(1, 2), chatId, respondMessage);
				} else {
					respondMessage.append("Invalid command.. example /edit 1000.5 1050 ");
				}
				
				
			}

			if (!respondMessage.toString().isEmpty()) {
				messageToSend.addMessage(chatId, respondMessage.toString());
			} else {
				System.out.println("Respond message is empty");
			}


		}

	}
	
	private String[] trimCommand(String command) {
		// replace multiple spaces with 1, split by spaces and skip the 1st string
		return command.trim().replaceAll("\\s+", " ").split(" ");
	}

	// return true if c is between a and b

	/**
	 * Check if C is between A and B
	 */
	private boolean isBetween(double a, double b, double c) {
		return b > a ? c > a && c < b : c > b && c < a;
	}
	
	private void addToWatchlist(List<String> priceList, int chatId, StringBuilder respondMessage) {
		if (!priceList.isEmpty()) {
			for (String s : priceList) {
				if (NumberUtils.isParsable(s) && Double.valueOf(s) > 0) {
					Double tmpPrice = Double.valueOf(s);
					if (tmpPrice % 1 == 0 || (Math.round(tmpPrice * 10) / 10.0) % .5 == 0) {
						if (isBetween(0, price.getPrice() * 2, tmpPrice)) {
							if (priceWatchList.getPrices().containsKey(tmpPrice)) {
								// no duplicates to chatId list
								if (!priceWatchList.getPrices().get(tmpPrice).contains(chatId)) {
									add(chatId, tmpPrice);
									respondMessage.append("Added ").append(tmpPrice).append(" to watchlist \n");
									priceDataService.addToExisting(tmpPrice, chatId);
								}
							} else {
								add(chatId, tmpPrice);
								priceDataService.addNew(tmpPrice, chatId);
								respondMessage.append("Added ").append(tmpPrice).append(" to watchlist \n");
							}
						} else {
							respondMessage.append(s).append(" out of range 0 - ").append(tmpPrice * 2).append("\n");
						}
					} else {
						respondMessage.append("[").append(s).append("]").append(" must be exact number or end with .5\n");
					}
				} else {
					respondMessage.append("[").append(s).append("]").append(" not a number (must use . instead of , ) or is negative \n");
				}

			}

		} else {
			respondMessage.append("Invalid command.. example /add 3500.5");
		}
	}
	
	private void removeFromWatchlist(List<String> priceList, int chatId, StringBuilder respondMessage) {
		if (!priceList.isEmpty()) {
			for (String s : priceList) {
				if (StringUtils.isNumeric(s) && Double.valueOf(s) > 0) {
					Double tmpPrice = Double.valueOf(s);
					if (priceWatchList.getPrices().containsKey(tmpPrice) && priceWatchList.getPrices().get(tmpPrice).contains(chatId)) {
						remove(chatId, tmpPrice);
						respondMessage.append("Removed ").append(tmpPrice).append(" from watchlist\n");
						priceDataService.removeChatId(tmpPrice, chatId);
					}
				} else {
					respondMessage.append("[").append(s).append("]").append(" not a number (must use . instead of , ) or is negative \n");
				}

			}
		} else {
			respondMessage.append("Invalid command.. example /remove 3500");
		}
	}

	private void add(int chatId, Double price) {
		this.priceWatchList.addChatIdToPrice(price, chatId);
		System.out.println("*Add* added " + chatId + " to " + price + " watchlist value list");
	}

	private void remove(int chatId, Double price) {
		this.priceWatchList.removeChatIdFromPrice(chatId, price);
		System.out.println("*Remove* Removed " + chatId + " from " + price + " watchlist value list");
	}

	@Override
	public String getBotUsername() {
		return username;
	}

	@Override
	public String getBotToken() {
		return token;
	}


}
