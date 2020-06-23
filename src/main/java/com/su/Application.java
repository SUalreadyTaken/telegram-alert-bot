package com.su;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.ApiContextInitializer;

import javax.annotation.PostConstruct;

@Controller
@SpringBootApplication
@EnableScheduling
public class Application implements CommandLineRunner {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Override
  public void run(String... args) {

  }

  @PostConstruct
  public void start() {
    ApiContextInitializer.init();
  }
}
