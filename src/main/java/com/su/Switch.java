package com.su;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.su.Runnable.CheckPrice;
import com.su.Runnable.ExecuteMessages;
import com.su.Runnable.PreventIdling;
import com.su.Service.IdleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Set;

import static java.lang.Thread.*;

@Component
public class Switch {

	@Value("${heroku.alternative.website}")
	private String herokuAlternativeWebsite;

	@Value("${switchapp}")
	private boolean switchApp;

	private final ScheduledAnnotationBeanPostProcessor postProcessor;

	private final CheckPrice checkPrice;
	private final ExecuteMessages executeMessages;
	private final PreventIdling preventIdling;
	private final IdleService idleService;

	private static final String checkPriceTask = "checkPrice";
	private static final String executeMessagesTask = "executeMessages";
	private static final String preventIdlingTask = "preventIdling";
	//DBController can run

	private final Init init;

	private final ObjectMapper objectMapper;

	@Autowired
	public Switch(ScheduledAnnotationBeanPostProcessor postProcessor, CheckPrice checkPrice, ExecuteMessages executeMessages,
				  PreventIdling preventIdling, IdleService idleService, Init init, ObjectMapper objectMapper) {
		this.postProcessor = postProcessor;
		this.checkPrice = checkPrice;
		this.executeMessages = executeMessages;
		this.preventIdling = preventIdling;
		this.idleService = idleService;
		this.init = init;
		this.objectMapper = objectMapper;
	}

	private void stopSchedule() {
		postProcessor.postProcessBeforeDestruction(checkPrice, checkPriceTask);
		postProcessor.postProcessBeforeDestruction(executeMessages, executeMessagesTask);
		postProcessor.postProcessBeforeDestruction(preventIdling, preventIdlingTask);
	}

	private void startSchedule() {
		postProcessor.postProcessAfterInitialization(checkPrice, checkPriceTask);
		postProcessor.postProcessAfterInitialization(executeMessages, executeMessagesTask);
		postProcessor.postProcessAfterInitialization(preventIdling, preventIdlingTask);
	}

	public String listSchedules() throws JsonProcessingException {
		Set<ScheduledTask> setTasks = postProcessor.getScheduledTasks();
		if (!setTasks.isEmpty()) {
			return objectMapper.writeValueAsString(setTasks);
		} else {
			return "No running tasks !";
		}
	}

	// switches to alternative app every sunday at 3 am
	@Scheduled(cron = "0 45 10 * * TUE")
	private void switchApp() throws InterruptedException, IOException {
		if (switchApp) {
			// stop alertbot
			this.init.registerOrStop();
			idleService.switchAlternativeBoolean();
			stopSchedule();

			//try to wake the sleeping alternative app for 5 min
			for (int i = 0; i < 10; i++) {
				URL u = new URL(herokuAlternativeWebsite);
				HttpURLConnection connection = (HttpURLConnection) u.openConnection();
				connection.setRequestMethod("HEAD");
				//alternative app woke up
				if (connection.getResponseCode() == 200) {
					connection.disconnect();
					return;
				}
				connection.disconnect();
				sleep(30000);
			}

			//if it got this far then the alternative app failed to wake up
			//register alertbot again
			//start all the schedules again and continue to use this app
			idleService.switchAlternativeBoolean();
			startSchedule();
			this.init.registerOrStop();
		}
	}

}
