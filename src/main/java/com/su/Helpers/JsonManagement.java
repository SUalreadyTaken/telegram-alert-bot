package com.su.Helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.su.Model.Data;
import com.su.Model.PriceWatchList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class JsonManagement {

    @Value("${json.data}")
    private String json;

    private ObjectMapper objectMapper = new ObjectMapper();

    private PriceWatchList priceWatchList;

    @Autowired
    public JsonManagement(PriceWatchList priceWatchList) {
        this.priceWatchList = priceWatchList;
    }

    public void priceWatchListSetup() {
        try {
            File jsonFile = new File(json);
            if (jsonFile.exists() && jsonFile.length() != 0) {
                Data[] priceList = objectMapper.readValue(new File(json), Data[].class);
                for (Data data : priceList) {
                    double tmpPrice = data.getPrice();
                    for (Long chatId : data.getChatIds()) {
                        priceWatchList.addChatIdToPrice(tmpPrice, chatId);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Write priceWatchList to json file
    // Performed every minute
    @Scheduled(fixedDelay = 60*1000)
    public void saveData() {
        System.out.println("Trying to save data");
        try {
            List<Data> dataList = new ArrayList<>();
            for (Map.Entry<Double, List<Long>> price: priceWatchList.getPrices().entrySet()) {
                Data tmpData = new Data(price.getKey(), price.getValue());
                dataList.add(tmpData);
            }
            objectMapper.writeValue(new File(json), dataList);
            dataList = null;
            System.out.println("Saved data");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
