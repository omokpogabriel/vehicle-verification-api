package com.naijavehicle.api.repositoryService;

import com.naijavehicle.api.models.LoginDetails;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LoginDetailsRepository extends MongoRepository<LoginDetails, String> {
    long countByUsernameAndStatusAndTimestampAfter(String username, String status, LocalDateTime after);
}
