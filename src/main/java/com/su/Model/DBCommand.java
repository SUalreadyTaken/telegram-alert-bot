package com.su.Model;

import java.util.List;

public class DBCommand {

	private DBCommandType dbCommandType;
	private double price;
	private List<Integer> chatIds;

	public DBCommand(DBCommandType dbCommandType, double price, List<Integer> chatIds) {
		this.dbCommandType = dbCommandType;
		this.price = price;
		this.chatIds = chatIds;
	}

	public DBCommandType getDbCommandType() {
		return dbCommandType;
	}

	public void setDbCommandType(DBCommandType dbCommandType) {
		this.dbCommandType = dbCommandType;
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}

	public List<Integer> getChatIds() {
		return chatIds;
	}

	public void setChatIds(List<Integer> chatIds) {
		this.chatIds = chatIds;
	}
}
