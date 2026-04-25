package com.naijavehicle.api.models;

import com.naijavehicle.api.dto.ScrapingResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "vehicle_reports") // Standard snake_case for collections
@Data
@Builder // Useful for creating reports in your service
@NoArgsConstructor
@AllArgsConstructor
public class VehicleReport {

    @Id
    private String id;

    @Indexed(unique = true)
    private String plateNumber;

    // The key is the source (e.g., "NIID"), the value is the result object
    private List<ScrapingResult<?>> results;

    private String ipAddress;
    private String appInstallationId;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Version
    private Long version; // Optimistic locking to prevent concurrent write issues
}
