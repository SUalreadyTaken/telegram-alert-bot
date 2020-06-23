package com.su;

import com.su.Model.*;
import com.su.Runnable.CheckPrice;
import com.su.Runnable.ExecuteMessages;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.*;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class TestAlertBot {

  @InjectMocks
  private AlertBot alertBot;
  @InjectMocks
  private CheckPrice checkPrice;
  @InjectMocks
  private ExecuteMessages executeMessages;

  @Spy
  private PriceWatchList priceWatchList;
  @Spy
  private MessageToSend messageToSend;
  @Spy
  private Price price;
  // need dbCommandsQueue DONT DELETE
  @Spy
  private DBCommandsQueue dbCommandsQueue;

  private final UpdateToSend updateToSend = new UpdateToSend();

  @Before
  public void setUp() {
    try {
      InputStream input = new FileInputStream("src/main/resources/application.properties");
      Properties properties = new Properties();
      properties.load(input);
      String bitmexUrl = properties.getProperty("bitmex");
      ReflectionTestUtils.setField(checkPrice, "MEX", bitmexUrl);
    } catch (Throwable ex) {
      System.out.println("Error in TestAlertBot.setUp()");
      System.out.println(ExceptionUtils.getStackTrace(ex));
    }
  }

  /**
   * Checkprice gets the latest price from bitmex api and sets it to oldPrice
   * field
   */
  @Test
  public void checkPrice_GetsPrice() {
    checkPrice.run();
    double oldPrice = (double) ReflectionTestUtils.getField(checkPrice, "oldPrice");
    double cannotBe = 0;
    assertTrue(oldPrice > cannotBe);
  }

  /**
   * Adds price to chats watchlist
   */
  @Test
  public void alertBot_Add() throws IOException {
    price.setPrice(200);
    alertBot.onUpdatesReceived(Collections.singletonList(updateToSend.createUpdate(1, "/add 100")));
    alertBot.onUpdatesReceived(Collections.singletonList(updateToSend.createUpdate(2, "/add 100")));

    assertEquals(priceWatchList.getPrices().size(), 1);
    assertEquals(priceWatchList.getPrices().get(100.0).size(), 2);

  }

  /**
   * Removes the price from chats watchlist
   */
  @Test
  public void alertBot_Remove() throws IOException {
    price.setPrice(100);
    List<Update> updateList = asList(updateToSend.createUpdate(1, "/add 100"), updateToSend.createUpdate(2, "/add 100"),
        updateToSend.createUpdate(3, "/add 100"), updateToSend.createUpdate(3, "/remove 100"),
        updateToSend.createUpdate(2, "/remove 100"));
    alertBot.onUpdatesReceived(updateList);
    assertEquals(priceWatchList.getPrices().get(100.0).size(), 1);
    assertEquals(priceWatchList.getPrices().size(), 1);
  }

  /**
   * Removes first price from watchlist and adds the second
   */
  @Test
  public void alertBot_Edit() throws IOException {
    price.setPrice(100);
    List<Update> updateList = asList(updateToSend.createUpdate(1, "/add 100"),
        updateToSend.createUpdate(1, "/edit 100 200"));
    alertBot.onUpdatesReceived(updateList);
    assertNull(priceWatchList.getPrices().get(100.0));
    assertEquals(priceWatchList.getPrices().get(200.0).size(), 1);
  }

  /**
   * If price to be removed from watchlist doesnt exist it still adds the price
   * you wanted to edit it to
   */
  @Test
  public void alertBotEdit_whenNothingToRemove() throws IOException {
    price.setPrice(100);
    List<Update> updateList = Collections.singletonList(updateToSend.createUpdate(1, "/edit 100 200"));
    alertBot.onUpdatesReceived(updateList);
    assertEquals(priceWatchList.getPrices().get(200.0).size(), 1);
  }

  /**
   * CheckPrice adds alerts to messageToSend and deletes those entries from
   * watchlist
   */
  @Test
  public void checkPrice_DeletesPriceFromWatchlist() throws IOException {
    price.setPrice(100);
    List<Update> updateList = asList(updateToSend.createUpdate(1, "/add 100"), updateToSend.createUpdate(2, "/add 100"),
        updateToSend.createUpdate(3, "/add 105"), updateToSend.createUpdate(4, "/add 110"),
        updateToSend.createUpdate(5, "/add 115"), updateToSend.createUpdate(6, "/add 120"));
    alertBot.onUpdatesReceived(updateList);
    ReflectionTestUtils.setField(checkPrice, "lowPrice", 100);
    ReflectionTestUtils.setField(checkPrice, "highPrice", 105);
    checkPrice.checkWatchlist(price.getPrice());
    assertNull(priceWatchList.getPrices().get(100.0));
    assertNull(priceWatchList.getPrices().get(105.0));
    assertEquals(priceWatchList.getPrices().size(), 3);
    // 6 added 3 price alerts
    assertEquals(messageToSend.getMessageQueue().size(), 9);
  }

  /**
   * ExecuteMessages needs to sleep for 1 second if 30 messages are sent within 1
   * second
   */
  @Test
  public void executeMessages_Sleeps() {
    long start = System.currentTimeMillis();
    ReflectionTestUtils.setField(executeMessages, "LAST_MESSAGE_SENT", start);
    for (int i = 0; i < 33; i++) {
      executeMessages.sleepIfNeeded();
    }
    assertTrue(System.currentTimeMillis() - start > 1000);

  }

  /**
   * ExecuteMessages doesnt sleep and changes lastSendMessage if 1 second has
   * passed.
   */
  @Test
  public void executeMessages_DoesntSleep() {
    long start = System.currentTimeMillis();
    ReflectionTestUtils.setField(executeMessages, "LAST_MESSAGE_SENT", start);
    for (int i = 0; i < 3; i++) {
      executeMessages.sleepIfNeeded();
    }
    assertTrue(System.currentTimeMillis() - start < 1000);

  }

  /**
   * For fun to see if any errors pop up
   */
  @Test
  public void simulateApp() throws BrokenBarrierException, InterruptedException {

    // rand.nextInt((max - min) + 1) + min;

    Random r = new Random();
    ReflectionTestUtils.setField(checkPrice, "oldPrice", 1);
    int maxPrice = 50;
    int minPrice = 1;
    price.setPrice((double) maxPrice / 2);
    int tests = 10;
    int testsInsideTest = 10;
    int maxUsers = 10;
    int minUsers = 1;
    CyclicBarrier cyclicBarrier = new CyclicBarrier(3);

    for (int i = 0; i < tests; i++) {
      int updateCount = r.nextInt((maxUsers - minUsers) + 1) + minUsers;
      Thread preAlertBot = new Thread(() -> {
        try {
          List<Update> preUpdatesThread = new ArrayList<>();
          for (int j = 0; j < updateCount; j++) {
            int userPrice = r.nextInt((maxPrice - minPrice) + 1) + minPrice;
            int userId = r.nextInt((maxUsers - minUsers) + 1) + minUsers;
            preUpdatesThread.add(updateToSend.createUpdate(userId, "/add " + userPrice));
          }
          alertBot.onUpdatesReceived(preUpdatesThread);
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
      preAlertBot.start();
      preAlertBot.join();
      System.out.println("TEST " + i + " updateCount >> " + updateCount + " \n\n After Pre \n");
      for (int x = 0; x < testsInsideTest; x++) {
        int low = r.nextInt((maxPrice - minPrice) + 1) + minPrice;
        int high = (int) (low * 1.1);

        ReflectionTestUtils.setField(checkPrice, "lowPrice", low);
        ReflectionTestUtils.setField(checkPrice, "highPrice", high);
        Thread alertBotThread = new Thread(() -> {
          try {
            cyclicBarrier.await();
          } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
          }
          try {
            int updateCountThread = r.nextInt((maxUsers - minUsers) + 1) + minUsers;
            List<Update> preUpdatesThread = new ArrayList<>();
            System.out.println("Updates inside thread " + updateCount);
            for (int j = 0; j < updateCountThread; j++) {

              int userPrice = r.nextInt((low + minPrice) + 1) + minPrice;
              int userId = r.nextInt((maxUsers - minUsers) + 1) + minUsers;
              preUpdatesThread.add(updateToSend.createUpdate(userId, "/add " + userPrice));
            }
            alertBot.onUpdatesReceived(preUpdatesThread);
          } catch (IOException e) {
            e.printStackTrace();
          }
        });

        Thread checkPriceThread = new Thread(() -> {
          try {
            cyclicBarrier.await();
          } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
          }
          // price doesnt matter low and high does
          checkPrice.checkWatchlist(maxPrice);
        });

        boolean whoGoesFirst = r.nextBoolean();

        if (whoGoesFirst) {
          System.out.println("checkFIrst");
          checkPriceThread.start();
          alertBotThread.start();
          cyclicBarrier.await();
        } else {
          System.out.println("alertFirst");
          alertBotThread.start();
          checkPriceThread.start();
          cyclicBarrier.await();
        }

        checkPriceThread.join();
        alertBotThread.join();
        cyclicBarrier.reset();
        System.out.println("\n--TestInside #" + x + " END --\n");
      }
      System.out.println("\n\n----END----\n\n");
    }
  }

}
