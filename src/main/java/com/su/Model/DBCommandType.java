package com.su.Model;

public enum DBCommandType {
	REMOVEPRICE("removePrice"),
	ADDPRICE("addPrice"),
	REMOVECHATIDS("removeChatIds");

	private String dbCommand;

	DBCommandType(String dbCommand) {
		this.dbCommand = dbCommand;
	}

	public String getDBCommand() {
		return this.dbCommand;
	}
}
