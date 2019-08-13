package com.su;

import com.su.Model.*;
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

import static com.google.common.primitives.Ints.asList;

@Component
public class AlertBot extends TelegramLongPollingBot {

	private final Price price;
	private final PriceWatchList priceWatchList;
	private final MessageToSend messageToSend;
//	private final PriceDataService priceDataService;
	private final DBCommands dbCommands;

	@Value("${telegram.token}")
	private String token;

	@Value("${telegram.username}")
	private String username;

	@Autowired
	public AlertBot(Price price, PriceWatchList priceWatchList, MessageToSend messageToSend, DBCommands dbCommands) {
		this.price = price;
		this.priceWatchList = priceWatchList;
		this.messageToSend = messageToSend;
		this.dbCommands = dbCommands;
	}

	@Override
	public void onUpdateReceived(Update update) {

		if (update.hasMessage() && update.getMessage().hasText()) {

			System.out.println("Got this msg >> " + update.getMessage().getText());

			String command = update.getMessage().getText();
			int chatId = Math.toIntExact(update.getMessage().getChatId());
			StringBuilder respondMessage = new StringBuilder();

			if (command.startsWith("/start")) {
				double currentPrice = price.getPrice();
				respondMessage.append("Im online.. current btc price >> ").append(currentPrice);
				System.out.println("Im online.. current btc price >> " + currentPrice);
			}

			//yes works with chars added to command end.. /watchlistfoo
			if (command.startsWith("/watchlist") || command.startsWith("/add") || command.startsWith("/remove") || command.startsWith(
					"/edit")) {
				Map<Double, List<Integer>> tmpPriceWatchlist = priceWatchList.getPrices();
				synchronized (priceWatchList.getPrices()) {
					if (command.startsWith("/watchlist")) {
						getUserWatchlist(chatId, respondMessage, tmpPriceWatchlist);
					} else {
						String[] commandStringArray = trimCommand(command);
						String commandString = commandStringArray[0];
						List<String> commandPrices = Stream.of(commandStringArray).skip(1).collect(Collectors.toList());
						if (!commandPrices.isEmpty()) {
							switch (commandString) {
								case "/add":
									addToWatchlist(tmpPriceWatchlist, commandPrices, chatId, respondMessage);
									break;
								case "/remove":
									removeFromWatchlist(tmpPriceWatchlist, commandPrices, chatId, respondMessage);
									break;
								case "/edit":
									if (commandPrices.size() == 2) {
										removeFromWatchlist(tmpPriceWatchlist, commandPrices.subList(0, 1), chatId, respondMessage);
										if (respondMessage.toString().isEmpty()) {
											respondMessage.append("Nothing to remove\n");
										}
										addToWatchlist(tmpPriceWatchlist, commandPrices.subList(1, 2), chatId, respondMessage);
									} else {
										respondMessage.append("Invalid command.. example /edit 1000.5 1050 ");
									}
									break;
							}
						} else {
							respondMessage.append("Invalid command.. example /add 3500 or /remove 3500 or /edit 3500 3300");
						}

					}

				}

			}

			if (!respondMessage.toString().isEmpty()) {
				synchronized (messageToSend.getMessageList())  {
					messageToSend.addMessage(chatId, respondMessage.toString());
				}
			} else {
				System.out.println("Respond message is empty");
			}

		}

	}

	private void getUserWatchlist(int chatId, StringBuilder respondMessage, Map<Double, List<Integer>> tmpPriceWatchlist) {
		List<Double> unsortedWatchlist = new ArrayList<>();
		for (Map.Entry<Double, List<Integer>> watchlistPriceSet : tmpPriceWatchlist.entrySet()) {
			List<Integer> tmpChatIds = watchlistPriceSet.getValue();
			if (!tmpChatIds.isEmpty()) {
				if (tmpChatIds.contains(chatId)) {
					unsortedWatchlist.add(watchlistPriceSet.getKey());
				}
			}
		}
		unsortedWatchlist.sort(Collections.reverseOrder());
		for (Double watchListPrice : unsortedWatchlist) {
			respondMessage.append(watchListPrice).append("\n");
		}
		System.out.println("*Watchlist* sending watchlist to " + chatId + " watchlist is as follows \n" + respondMessage.toString());
	}

	private String[] trimCommand(String command) {
		// replace multiple spaces with 1, split by spaces
		return command.trim().replaceAll("\\s+", " ").split(" ");
	}

	private void addToWatchlist(Map<Double, List<Integer>> watchList, List<String> priceList, int chatId, StringBuilder respondMessage) {
		Map<Double, Integer> addChatIdToWatchlist = new HashMap<>();
		for (String s : priceList) {
			if (NumberUtils.isParsable(s) && Double.valueOf(s) > 0) {
				Double tmpPrice = Double.valueOf(s);
				if (tmpPrice % 1 == 0 || (Math.round(tmpPrice * 10) / 10.0) % .5 == 0) {
					if (isBetweenZeroAndPrice(tmpPrice)) {
						if (watchList.containsKey(tmpPrice)) {
							// no duplicates to chatId list
							if (!watchList.get(tmpPrice).contains(chatId)) {
								add(chatId, tmpPrice);
								respondMessage.append("Added ").append(tmpPrice).append(" to watchlist \n");
//								priceDataService.addToExisting(tmpPrice, chatId);
							}
						} else {
							add(chatId, tmpPrice);
//							priceDataService.addNew(tmpPrice, chatId);
							respondMessage.append("Added ").append(tmpPrice).append(" to watchlist \n");
						}
						addChatIdToWatchlist.put(tmpPrice, chatId);
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
		if (!addChatIdToWatchlist.isEmpty()) {
			synchronized (dbCommands.getAddPriceToChatIdWatchlist()) {
				for (Map.Entry<Double, Integer> entry : addChatIdToWatchlist.entrySet()) {
					if (dbCommands.getAddPriceToChatIdWatchlist().containsKey(entry.getKey())) {
						dbCommands.getAddPriceToChatIdWatchlist().get(entry.getKey()).add(entry.getValue());
					} else {
						dbCommands.getAddPriceToChatIdWatchlist().put(entry.getKey(), Stream.of(entry.getValue()).collect(Collectors.toList()));
					}
				}
			}
		}
	}

	private void removeFromWatchlist(Map<Double, List<Integer>> watchList, List<String> priceList, int chatId,
									 StringBuilder respondMessage) {
		Map<Double, Integer> removeChatIdFromWatchlist = new HashMap<>();
		for (String s : priceList) {
			if (StringUtils.isNumeric(s) && Double.valueOf(s) > 0) {
				Double tmpPrice = Double.valueOf(s);
				if (watchList.containsKey(tmpPrice) && watchList.get(tmpPrice).contains(chatId)) {
					remove(chatId, tmpPrice);
					removeChatIdFromWatchlist.put(tmpPrice, chatId);
					respondMessage.append("Removed ").append(tmpPrice).append(" from watchlist\n");
				}
			} else {
				respondMessage.append("[").append(s).append("]").append(" not a number (must use . instead of , ) or is negative \n");
			}
		}
		if (!removeChatIdFromWatchlist.isEmpty()) {
			synchronized (dbCommands.getRemovePriceFromChatIdWatchlist()) {
				for (Map.Entry<Double, Integer> entry : removeChatIdFromWatchlist.entrySet()) {
					if (dbCommands.getRemovePriceFromChatIdWatchlist().containsKey(entry.getKey())) {
						dbCommands.getRemovePriceFromChatIdWatchlist().get(entry.getKey()).add(entry.getValue());
					} else {
						dbCommands.getRemovePriceFromChatIdWatchlist().put(entry.getKey(), Stream.of(entry.getValue()).collect(Collectors.toList()));
					}
				}
			}
		}
	}

	private void add(int chatId, Double price) {
		this.priceWatchList.addChatIdToPrice(price, chatId);
		System.out.println("*Add* added " + price + " to " + chatId + " watchlist");
	}

	private void remove(int chatId, Double price) {
		this.priceWatchList.removeChatIdFromPrice(chatId, price);
		System.out.println("*Remove* Removed " + price + " from " + chatId + " watchlist");
	}

	private boolean isBetweenZeroAndPrice(double a) {
		return a > 0 && price.getPrice() * 2 >= a;
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
