package com.su.Service;

import com.su.Model.PriceData;
import com.su.Repository.PriceDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class PriceDataService {

  private final PriceDataRepository priceDataRepository;

  public PriceDataService(PriceDataRepository priceDataRepository) {
    this.priceDataRepository = priceDataRepository;
  }

  public void add(Double price, int chatId) {
    Optional<PriceData> optionalPriceData = priceDataRepository.findByPrice(price);
    if (optionalPriceData.isPresent()) {
      PriceData priceData = optionalPriceData.get();
      priceData.getChatIds().add(chatId);
      priceDataRepository.save(priceData);
    } else {
      priceDataRepository.save(new PriceData(price, Stream.of(chatId).collect(Collectors.toList())));
    }
  }

  public void removeChatId(Double price, int chatId) {
    Optional<PriceData> optionalPriceData = priceDataRepository.findByPrice(price);
    if (optionalPriceData.isPresent()) {
      PriceData priceData = optionalPriceData.get();
      priceData.getChatIds().removeIf(integer -> integer == chatId);
      if (priceData.getChatIds().isEmpty()) {
        priceDataRepository.delete(priceData);
      } else {
        priceDataRepository.save(priceData);
      }
    }
  }

  public void removeChatIdsFromPrice(Double price, List<Integer> chatIdList) {
    Optional<PriceData> optionalPriceData = priceDataRepository.findByPrice(price);
    if (optionalPriceData.isPresent()) {
      PriceData priceData = optionalPriceData.get();
      if (priceData.getChatIds().size() == chatIdList.size() && priceData.getChatIds().containsAll(chatIdList)) {
        priceDataRepository.delete(priceData);
      } else {
        chatIdList = chatIdList.stream().distinct().collect(Collectors.toList());
        for (int chatId : chatIdList) {
          priceData.getChatIds().removeIf(integer -> integer == chatId);
        }
        priceDataRepository.save(priceData);
      }
    }
  }

}
