package com.naijavehicle.api.repositoryService;

import com.naijavehicle.api.models.LoginDetails;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginDetailsRepository extends MongoRepository<LoginDetails, String> {
}
