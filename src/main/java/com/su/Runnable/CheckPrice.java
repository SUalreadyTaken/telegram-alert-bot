package com.su.Runnable;

import com.su.Model.MessageToSend;
import com.su.Model.Price;
import com.su.Model.PriceWatchList;
import com.su.Service.PriceDataService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class CheckPrice {

	@Value("${bitmex}")
	private String MEX;

	private int EVERY_MINUTE = 0;
	private double oldPrice = 0;
	private long lastRequestTime = 0;

	private final Price price;
	private final MessageToSend messageToSend;
	private final PriceWatchList priceWatchList;
	private final PriceDataService priceDataService;

	@Autowired
	public CheckPrice(Price price, MessageToSend messageToSend, PriceWatchList priceWatchList, PriceDataService priceDataService) {
		this.price = price;
		this.messageToSend = messageToSend;
		this.priceWatchList = priceWatchList;
		this.priceDataService = priceDataService;
	}

	@PostConstruct
	private void firstPrice() {
		oldPrice = getPrice();
		lastRequestTime = System.currentTimeMillis();
		checkWatchlist(oldPrice);
	}

	@Scheduled(fixedDelay = 2000)
	public void run() {
		// Rate limit is 30 per 1 min
		// usually thread runs every 2sec but occasionally its few ms faster and it will add up
		// so check to be safe
		try {
			TimeUnit.MILLISECONDS.sleep((System.currentTimeMillis() - lastRequestTime) - 2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.out.println("Error in CheckPrice.run failed to sleep");
		}

		double currentPrice = getPrice();
		lastRequestTime = System.currentTimeMillis();

		// price changed check if i need to send an alert
		if (oldPrice != 0 && currentPrice != 0 && oldPrice != currentPrice) {
			checkWatchlist(currentPrice);
		}
	}

	/**
	 * Checks if it's necessary to send out alerts.
	 * If so
	 * Adds prices to MessageToSend
	 * Removes prices from watchlist and database
	 *
	 * @param currentPrice Price to check against watchlist
	 */
	private void checkWatchlist(double currentPrice) {
		price.setPrice(currentPrice);
		// go through watchList
		for (Double watchlistPrice : priceWatchList.getPrices().keySet()) {
			// if price on the watchlist is equals or in between current and old btc price
			if (Objects.equals(watchlistPrice, currentPrice) || (isBetween(currentPrice, oldPrice, watchlistPrice) && oldPrice != 0)) {
				// chatIdsList
				Iterator chatIdsIterator = priceWatchList.getPrices().get(watchlistPrice).iterator();
				List<Integer> chatIdsToRemoveList = new ArrayList<>();
				// add all chatIds to sendList
				while (chatIdsIterator.hasNext()) {
					String text;
					int chatId = (int) chatIdsIterator.next();
					if (oldPrice > currentPrice) {
						text = "ALERT price fell below >> " + watchlistPrice;
					} else {
						text = "ALERT price rose above >> " + watchlistPrice;
					}
					System.out.println("Sending alert to " + chatId + " with text >> " + text);
					messageToSend.addMessage(chatId, text);
					chatIdsToRemoveList.add(chatId);
					chatIdsIterator.remove();
				}
				priceDataService.removeOrDelete(watchlistPrice, chatIdsToRemoveList);
			}
		}
		// remove empty keys
		priceWatchList.getPrices().entrySet().removeIf(price -> price.getValue().isEmpty());
		oldPrice = currentPrice;
	}

	/**
	 * @return Current bitmex xbt price
	 */
	private double getPrice() {
		double newPrice = 0;
		try {
			URL url = new URL(MEX);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.connect();

			// "If you are limited, you will receive a 429 response and an additional header"
			// Sleep 60sec
			if (connection.getResponseCode() == 429) {
				System.out.println("Limit reached sleep 60 sec");
				TimeUnit.MILLISECONDS.sleep(60000);
			}

			String JSON_STRING = new Scanner(connection.getInputStream(), "UTF-8").next();
			JSONArray jsonArray = new JSONArray(JSON_STRING);

			if (!jsonArray.isEmpty()) {
				JSONObject jsonObject = (JSONObject) jsonArray.get(0);
				newPrice = Double.parseDouble(jsonObject.get("price").toString());
				EVERY_MINUTE++;
				if (EVERY_MINUTE == 30) {
					System.out.println("Reminder Current price >> " + newPrice);
					EVERY_MINUTE = 0;
				}
				connection.disconnect();
			}
		} catch (Throwable ex) {
			System.out.println("Error in CheckPrice.getPrice");
			System.out.println(ExceptionUtils.getStackTrace(ex));
		}
		return newPrice;
	}

	/**
	 * Check if C is between A and B
	 */
	private boolean isBetween(double a, double b, double c) {
		return b > a ? c > a && c < b : c > b && c < a;
	}

}
