package com.su;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

class UpdateToSend {

  private final ObjectMapper objectMapper = new ObjectMapper();

  Update createUpdate(int chatId, String text) throws IOException {

    String json = "{\"updateId\":666,\"message\":{\"messageId\":99,\"from\":{\"id\":" + chatId
        + ",\"firstName\":\"Tester\"," + "\"isBot\":false,\"languageCode\":\"en\"},\"date\":"
        + (System.currentTimeMillis() / 1000L) + ",\"chat\":{\"id\":" + chatId + ",\"type\":\"private\","
        + "\"firstName\":\"Tester\"},\"text\":\"" + text + "\"}}";

    Reader reader = new StringReader(json);

    return objectMapper.readValue(reader, Update.class);
  }

}
