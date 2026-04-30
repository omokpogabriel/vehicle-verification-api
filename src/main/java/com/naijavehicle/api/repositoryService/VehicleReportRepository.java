package com.naijavehicle.api.repositoryService;

import com.naijavehicle.api.models.VehicleReport;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
public class VehicleReportRepository {

    private final MongoTemplate mongoTemplate;



    public void saveReport(VehicleReport vehicleReport) {

        Query query = new Query(Criteria.where("plateNumber").is(vehicleReport.getPlateNumber())
                .and("updatedAt").gte(LocalDate.now().atStartOfDay()));

        Update update = new Update()
                .setOnInsert("plateNumber", vehicleReport.getPlateNumber())
                .setOnInsert("appInstallationId", vehicleReport.getAppInstallationId())
                .setOnInsert("ipAddress", vehicleReport.getIpAddress())
                .setOnInsert("userId", vehicleReport.getUserId())
                .setOnInsert("results", vehicleReport.getResults());

        mongoTemplate.upsert(query, update, VehicleReport.class);
    }

    public Page<VehicleReport> findByUserId(String userId, Pageable pageable) {
        Query query = new Query(Criteria.where("userId").is(userId));
        long count = mongoTemplate.count(query, VehicleReport.class);
        
        query.with(pageable);
        java.util.List<VehicleReport> list = mongoTemplate.find(query, VehicleReport.class);
        
        return new PageImpl<>(list, pageable, count);
    }

}
