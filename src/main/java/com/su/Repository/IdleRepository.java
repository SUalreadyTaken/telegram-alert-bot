package com.su.Repository;

import com.su.Model.Idle;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IdleRepository extends MongoRepository<Idle, String> {
}
