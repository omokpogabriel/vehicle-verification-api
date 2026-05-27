package com.naijavehicle.api.repositoryService;

import com.mongodb.DuplicateKeyException;
import com.naijavehicle.api.models.VehicleReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleReportRepository {

    private final MongoTemplate mongoTemplate;



    public void saveReport(VehicleReport vehicleReport) {

        Query query = new Query(Criteria.where("plateNumber").is(vehicleReport.getPlateNumber()));

        Update update = new Update()
                .setOnInsert("plateNumber", vehicleReport.getPlateNumber())
                .setOnInsert("appInstallationId", vehicleReport.getAppInstallationId())
                .setOnInsert("ipAddress", vehicleReport.getIpAddress())
                .setOnInsert("userId", vehicleReport.getUserId())
                .setOnInsert("results", vehicleReport.getResults());

        try{
           boolean plateNumberExists =  mongoTemplate.exists(query, VehicleReport.class);

           if(!plateNumberExists){
               mongoTemplate.save(vehicleReport);
           }else{
               mongoTemplate.upsert(query, update, VehicleReport.class);
           }

        }catch (DuplicateKeyException ex){
            log.info("the dupl exception -> {}", vehicleReport.getPlateNumber());
            mongoTemplate.upsert(query, update, VehicleReport.class);
        }

    }

    public VehicleReport updateReport(VehicleReport report){
        Query query = new Query((Criteria.where("plateNumber").is(report.getPlateNumber())));

        return mongoTemplate.findAndModify(query, new Update().set("updatedAt", LocalDate.now().atStartOfDay())
                .set("results", report.getResults()), VehicleReport.class);
    }

    public Page<VehicleReport> findByUserId(String userId, Pageable pageable) {
        Query query = new Query(Criteria.where("userId").is(userId));
        long count = mongoTemplate.count(query, VehicleReport.class);
        
        query.with(pageable);
        java.util.List<VehicleReport> list = mongoTemplate.find(query, VehicleReport.class);
        
        return new PageImpl<>(list, pageable, count);
    }

    public VehicleReport findByPlateNumber(String plateNumber){
        Query query = new Query(Criteria.where("plateNumber").is(plateNumber));
        var result =  mongoTemplate.findOne(query, VehicleReport.class);
        return result;
    }

}
