package com.su.Model;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DBCommands {

	/**
	 * Used in AlertBot to remove price from chatId watchlist
	 */
	private Map<Double, List<Integer>> removePriceFromChatIdWatchlist = Collections.synchronizedMap(new LinkedHashMap<>());

	/**
	 * Used in AlertBot to add price to chatId watchlist
	 */
	private Map<Double, List<Integer>> addPriceToChatIdWatchlist = Collections.synchronizedMap(new LinkedHashMap<>());

	/**
	 * Used in CheckPrice to remove chatId from price watchlist
	 */
	private Map<Double, List<Integer>> removeChatIdFromPrice = Collections.synchronizedMap(new LinkedHashMap<>());

	public Map<Double, List<Integer>> getRemovePriceFromChatIdWatchlist() {
		return removePriceFromChatIdWatchlist;
	}

	public void setRemovePriceFromChatIdWatchlist(Map<Double, List<Integer>> removePriceFromChatIdWatchlist) {
		this.removePriceFromChatIdWatchlist = removePriceFromChatIdWatchlist;
	}

	public Map<Double, List<Integer>> getAddPriceToChatIdWatchlist() {
		return addPriceToChatIdWatchlist;
	}

	public void setAddPriceToChatIdWatchlist(Map<Double, List<Integer>> addPriceToChatIdWatchlist) {
		this.addPriceToChatIdWatchlist = addPriceToChatIdWatchlist;
	}

	public Map<Double, List<Integer>> getRemoveChatIdFromPrice() {
		return removeChatIdFromPrice;
	}

	public void setRemoveChatIdFromPrice(Map<Double, List<Integer>> removeChatIdFromPrice) {
		this.removeChatIdFromPrice = removeChatIdFromPrice;
	}
}
