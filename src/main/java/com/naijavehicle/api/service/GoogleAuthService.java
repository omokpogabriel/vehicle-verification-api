package com.naijavehicle.api.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@Slf4j
public class GoogleAuthService {

    @Value("${google.client-id}")
    private String googleClientId;

    private final RestClient restClient = RestClient.create();

    public GoogleUserInfo verifyIdToken(String idToken) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.get()
                    .uri("https://oauth2.googleapis.com/tokeninfo?id_token={token}", idToken)
                    .retrieve()
                    .body(Map.class);

            if (response == null) return null;

            String aud = (String) response.get("aud");
            String emailVerified = (String) response.get("email_verified");

            if (!googleClientId.equals(aud)) {
                log.warn("Google token audience mismatch: expected {}, got {}", googleClientId, aud);
                return null;
            }

            if (!"true".equals(emailVerified)) {
                log.warn("Google account email not verified for sub: {}", response.get("sub"));
                return null;
            }

            String name = response.get("name") != null ? (String) response.get("name") : "";
            return new GoogleUserInfo(
                    (String) response.get("sub"),
                    (String) response.get("email"),
                    name
            );
        } catch (Exception e) {
            log.error("Failed to verify Google ID token: {}", e.getMessage());
            return null;
        }
    }

    public record GoogleUserInfo(String sub, String email, String name) {}
}
