package com.su;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Controller;
import org.telegram.telegrambots.ApiContextInitializer;

@Controller
@SpringBootApplication
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        ApiContextInitializer.init();
        SpringApplication.run(Application.class, args);
    }

//    @RequestMapping("/")
//    String index() {
//        return "index";
//    }


}
