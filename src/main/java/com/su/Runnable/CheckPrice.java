package com.su.Runnable;

import com.su.Model.*;
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
	private double highPrice = 0;
	private double lowPrice = 0;
	private long lastRequestTime = System.currentTimeMillis();

	private final Price price;
	private final MessageToSend messageToSend;
	private final PriceWatchList priceWatchList;
	private final DBCommandsQueue dbCommandsQueue;

	@Autowired
	public CheckPrice(Price price, MessageToSend messageToSend, PriceWatchList priceWatchList, DBCommandsQueue dbCommandsQueue) {
		this.price = price;
		this.messageToSend = messageToSend;
		this.priceWatchList = priceWatchList;
		this.dbCommandsQueue = dbCommandsQueue;
	}

	@PostConstruct
	private void firstPrice() {
		oldPrice = getPrice();
		lastRequestTime = System.currentTimeMillis();
		checkWatchlist(oldPrice);
	}

	@Scheduled(fixedDelay = 100)
	public void run() {
		// Rate limit is 30 per 1 min
		try {
			TimeUnit.MILLISECONDS.sleep(2000 - (System.currentTimeMillis() - lastRequestTime));
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
		oldPrice = currentPrice;
	}

	public void checkWatchlist(double currentPrice) {
		price.setPrice(currentPrice);
		Set<Map.Entry<Double, List<Integer>>> tmpPriceWatchlist = priceWatchList.getPrices().entrySet();
		Map<Double, List<Integer>> dbPricesToRemove = new LinkedHashMap<>();
		List<Message> messageList = new ArrayList<>();

		synchronized (priceWatchList.getPrices()) {
			Iterator<Map.Entry<Double, List<Integer>>> watchlistEntry = tmpPriceWatchlist.iterator();
			while (watchlistEntry.hasNext()) {
				Map.Entry<Double, List<Integer>> tmpWatchlistEntry = watchlistEntry.next();
				double priceToCheck = tmpWatchlistEntry.getKey();
				if (isBetween(lowPrice, highPrice, priceToCheck)) {
					dbPricesToRemove.put(priceToCheck, tmpWatchlistEntry.getValue());
					for (Integer chatId : tmpWatchlistEntry.getValue()) {
						String text;
						if (oldPrice > currentPrice) {
							text = "ALERT price fell below >> " + priceToCheck;
						} else {
							text = "ALERT price rose above >> " + priceToCheck;
						}
						System.out.println("Sending alert to " + chatId + " with text >> " + text);
						messageList.add(new Message(chatId, text));
					}
					watchlistEntry.remove();
				}
			}
		}

		if (!dbPricesToRemove.isEmpty()) {
			for (Map.Entry<Double, List<Integer>> entry : dbPricesToRemove.entrySet()) {
				try {
					dbCommandsQueue.getDbCommandsQueue().put(new DBCommand(DBCommandType.REMOVECHATIDS, entry.getKey(), entry.getValue()));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			for (Message message : messageList) {
				try {
					messageToSend.getMessageQueue().put(message);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	/**
	 * Gets bitmex btc price.
	 * Returns 0 if response code is 429 meaning request limit reached (will sleep for 60sec)
	 * and if api returns 502 which happens from time to time.
	 *
	 * @return btc price
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
				connection.disconnect();
				return 0;
			}

			// bitmex api gives 502 from time to time
			if (connection.getResponseCode() == 502) {
				System.out.println("502");
				connection.disconnect();
				return 0;
			}

			String JSON_STRING = new Scanner(connection.getInputStream(), "UTF-8").next();
			JSONArray jsonArray = new JSONArray(JSON_STRING);

			if (!jsonArray.isEmpty()) {
				JSONObject jsonObject = (JSONObject) jsonArray.get(0);
				newPrice = Double.parseDouble(jsonObject.get("close").toString());
				highPrice = Double.parseDouble(jsonObject.get("high").toString());
				lowPrice = Double.parseDouble(jsonObject.get("low").toString());
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
	 * Check if C is between or equal to A and B
	 */
	private boolean isBetween(double a, double b, double c) {
		return b >= a ? c >= a && c <= b : c >= b && c <= a;
	}

}
