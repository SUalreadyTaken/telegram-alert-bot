package com.su.Runnable;

import com.su.Model.DBCommand;
import com.su.Service.PriceDataService;

import java.util.concurrent.BlockingQueue;

public class DBController implements Runnable {

	private BlockingQueue<DBCommand> commandsQueue;
	private final PriceDataService priceDataService;

	public DBController(BlockingQueue<DBCommand> commandsQueue, PriceDataService priceDataService) {
		this.commandsQueue = commandsQueue;
		this.priceDataService = priceDataService;
	}

	@Override
	public void run() {
		while (true) {
			try {
				DBCommand dbCommand = commandsQueue.take();
				switch (dbCommand.getDbCommandType()) {
					case ADDPRICE:
						priceDataService.add(dbCommand.getPrice(), dbCommand.getChatIds().get(0));
						break;
					case REMOVEPRICE:
						priceDataService.removeChatId(dbCommand.getPrice(), dbCommand.getChatIds().get(0));
						break;
					case REMOVECHATIDS:
						priceDataService.removeChatIdsFromPrice(dbCommand.getPrice(), dbCommand.getChatIds());
						break;
					default:
						break;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public BlockingQueue<DBCommand> getCommandsQueue() {
		return commandsQueue;
	}

	public void setCommandsQueue(BlockingQueue<DBCommand> commandsQueue) {
		this.commandsQueue = commandsQueue;
	}
}
