package com.naijavehicle.api.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    @Builder.Default
    private String userId = java.util.UUID.randomUUID().toString();

    @Indexed(unique = true)
    private String username;

    private String fullName;

    private String password;

    private String email;

    private String phone;

    private String ipAddress;

    private String appInstallationId;

    private String googleId;

    private String biometricToken;

    private String resetToken;

    private java.time.LocalDateTime resetTokenExpiry;

    @Builder.Default
    private int tokenVersion = 0;

    private Set<String> roles;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
