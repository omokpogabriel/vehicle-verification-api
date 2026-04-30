package com.naijavehicle.api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "login_details")
public class LoginDetails {

    @Id
    private String id;
    
    @Builder.Default
    private String loginId = java.util.UUID.randomUUID().toString();
    
    private String username;
    
    private String ipAddress;
    
    private String location;
    
    private String appInstallationId;
    
    private String status; // e.g., "SUCCESS" or "FAILURE"
    
    private String failureReason; // optional, if tracking why it failed
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
