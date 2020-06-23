package com.su.Repository;

import com.su.Model.PriceData;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface PriceDataRepository extends MongoRepository<PriceData, String> {

  Optional<PriceData> findByPrice(Double tmpPrice);
}
