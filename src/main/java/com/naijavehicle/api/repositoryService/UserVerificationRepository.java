package com.naijavehicle.api.repositoryService;

import com.naijavehicle.api.models.UserVerification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserVerificationRepository {

    private final MongoTemplate mongoTemplate;

    public void saveOrUpdate(String userId, String plateNumber) {
        Query query = new Query(
                Criteria.where("userId").is(userId).and("plateNumber").is(plateNumber));
        Update update = new Update()
                .set("userId", userId)
                .set("plateNumber", plateNumber)
                .set("verifiedAt", LocalDateTime.now());
        mongoTemplate.upsert(query, update, UserVerification.class);
    }

    public Page<UserVerification> findByUserId(String userId, Pageable pageable) {
        Query query = new Query(Criteria.where("userId").is(userId));
        long count = mongoTemplate.count(query, UserVerification.class);
        query.with(pageable);
        List<UserVerification> list = mongoTemplate.find(query, UserVerification.class);
        return new PageImpl<>(list, pageable, count);
    }
}
